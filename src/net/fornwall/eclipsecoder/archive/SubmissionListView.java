package net.fornwall.eclipsecoder.archive;

import java.util.List;

import net.fornwall.eclipsecoder.preferences.EclipseCoderPlugin;
import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

class SolutionComparator extends ViewerSorter {
	public int column;

	public boolean reversed;

	public SolutionComparator(int column, boolean reversed) {
		this.column = column;
		this.reversed = reversed;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		Submission s1 = (Submission) e1;
		Submission s2 = (Submission) e2;
		int ret;
		switch (column) {
		case 0:
			ret = s1.getCoderHandle().compareTo(s2.getCoderHandle());
			break;
		case 1:
			ret = s1.getCoderNewRating() - s2.getCoderNewRating();
			break;
		case 2:
			ret = s1.getPoints() - s2.getPoints();
			break;
		case 3:
			ret = s1.getLanguage().compareTo(s2.getLanguage());
			break;
		case 4:
			ret = s1.getLevel() - s2.getLevel();
			break;
		default:
			throw new RuntimeException("Unknown column: " + column); //$NON-NLS-1$
		}
		return (reversed ? -1 : 1) * ret;
	}

}

class SolutionLabelProvider extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		Submission solution = (Submission) element;
		switch (columnIndex) {
		case 0:
			return solution.getCoderHandle();
		case 1:
			return String.valueOf(solution.getCoderNewRating());
		case 2:
			return String.valueOf(solution.getPoints() / 100) + '.' + String.valueOf(solution.getPoints() % 100);
		case 3:
			return solution.getLanguage();
		case 4:
			return String.valueOf(solution.getLevel());
		default:
			throw new RuntimeException("Unknown column: " + columnIndex); //$NON-NLS-1$
		}
	}

}

/**
 * A view which allows the user to browse and select problems for download and
 * project generation from the TopCoder problem archive.
 */
public class SubmissionListView extends ViewPart {
	static SubmissionListView instance;

	public static final String VIEW_ID = SubmissionListView.class.getCanonicalName();

	/**
	 * Update the problem list table by downloading the latest list from the
	 * TopCoder web site and displaying them in the problem list table.
	 */
	public static void showSolutions(List<Submission> solutions, int roundId, int problemId) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(SubmissionListView.VIEW_ID);
		} catch (PartInitException e) {
			Utilities.showException(e);
		}

		instance.viewer.setInput(solutions);
		instance.setRoundId(roundId);
		instance.setProblemId(problemId);
		for (int i = 0; i < instance.solutionListTable.getColumnCount(); i++) {
			instance.solutionListTable.getColumn(i).pack();
		}
	}

	int problemId;

	int roundId;

	Table solutionListTable;

	TableViewer viewer;

	@Override
	public void createPartControl(Composite parent) {
		SubmissionListView.instance = this;

		setContentDescription(Messages.selectProblemDescription);

		solutionListTable = new Table(parent, SWT.FULL_SELECTION);
		solutionListTable.setLinesVisible(true);

		viewer = new TableViewer(solutionListTable);
		viewer.setLabelProvider(new SolutionLabelProvider());
		viewer.setContentProvider(new ArrayContentProvider());

		for (String columnTitle : new String[] { Messages.columnNameCoder, Messages.columnNameRating,
				Messages.columnNamePoints, Messages.columnNameLanguage, Messages.columnNameLevel }) {
			TableColumn column = new TableColumn(solutionListTable, SWT.LEFT);
			column.setText(columnTitle);
		}

		viewer.setSorter(new SolutionComparator(0, false));
		solutionListTable.setSortColumn(solutionListTable.getColumn(0));
		solutionListTable.setSortDirection(SWT.DOWN);
		for (int i = 0; i < solutionListTable.getColumnCount(); i++) {
			final int index = i;
			solutionListTable.getColumn(i).addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					SolutionComparator previousSorter = (SolutionComparator) viewer.getSorter();
					boolean reversed = false;
					if (previousSorter.column == index) {
						reversed = !previousSorter.reversed;
					}
					solutionListTable.setSortColumn(solutionListTable.getColumn(index));
					solutionListTable.setSortDirection(reversed ? SWT.UP : SWT.DOWN);
					viewer.setSorter(new SolutionComparator(index, reversed));
				}
			});
			solutionListTable.getColumn(i).pack();
		}

		solutionListTable.setHeaderVisible(true);

		solutionListTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				final Submission submission = (Submission) ((IStructuredSelection) viewer.getSelection())
						.getFirstElement();
				if (submission == null) {
					return;
				}

				if (!EclipseCoderPlugin.isTcAccountSpecified()) {
					EclipseCoderPlugin.demandTcAccountSpecified();
					return;
				}

				Job job = new SubmissionFetcherJob(submission, roundId, problemId);
				job.setUser(true);
				job.schedule();
			}
		});
	}

	@Override
	public void setFocus() {
		solutionListTable.setFocus();
	}

	public void setProblemId(int problemId) {
		this.problemId = problemId;
	}

	public void setRoundId(int roundId) {
		this.roundId = roundId;
	}
}
