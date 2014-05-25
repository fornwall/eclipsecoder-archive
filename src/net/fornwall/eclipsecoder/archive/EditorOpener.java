package net.fornwall.eclipsecoder.archive;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import net.fornwall.eclipsecoder.util.Utilities;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * A class containing a single utility method for opening a read-only editor for
 * a String.
 */
public class EditorOpener {

	private static class StringStorage extends PlatformObject implements IStorage {
		private String input;

		private String name;

		public StringStorage(String input, String name) {
			this.input = input;
			this.name = name;
		}

		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(input.getBytes());
		}

		public IPath getFullPath() {
			return null;
		}

		public String getName() {
			return name;
		}

		public boolean isReadOnly() {
			return true;
		}

	}

	private static class StringStorageEditorInput extends PlatformObject implements IStorageEditorInput {
		private ImageDescriptor imageDescriptor;

		private StringStorage stringStorage;

		private String toolTipText;

		public StringStorageEditorInput(String inputString, String name, String toolTipText,
				ImageDescriptor imageDescriptor) {
			stringStorage = new StringStorage(inputString, name);
			this.toolTipText = toolTipText;
			this.imageDescriptor = imageDescriptor;
		}

		public boolean exists() {
			return true;
		}

		// hmm, this one is never called?
		public ImageDescriptor getImageDescriptor() {
			return imageDescriptor;
		}

		public String getName() {
			return stringStorage.getName();
		}

		public IPersistableElement getPersistable() {
			return null;
		}

		public IStorage getStorage() throws CoreException {
			return stringStorage;
		}

		public String getToolTipText() {
			return toolTipText;
		}
	}

	/**
	 * Open a read-only editor for the specified input String and input type.
	 * 
	 * @param input
	 *            The string to show in the editor
	 * @param name
	 *            The name of the editor
	 * @param toolTipText
	 *            The tool tip text for the editor
	 * @param fileExtension
	 *            The file extension used to find the appropriate editor
	 */
	public static void openEditor(String input, String name, String toolTipText, String fileExtension) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		String editorId = "org.eclipse.ui.DefaultTextEditor"; //$NON-NLS-1$
		ImageDescriptor imageDescriptor = null;
		IEditorDescriptor defaultEditorDescriptor = PlatformUI.getWorkbench().getEditorRegistry()
				.getDefaultEditor("." + fileExtension); //$NON-NLS-1$
		if (defaultEditorDescriptor != null) {
			editorId = defaultEditorDescriptor.getId();
			imageDescriptor = defaultEditorDescriptor.getImageDescriptor();
		}

		IStorageEditorInput editorInput = new StringStorageEditorInput(input, name, toolTipText, imageDescriptor);

		try {
			page.openEditor(editorInput, editorId);
		} catch (PartInitException e) {
			Utilities.showException(e);
		}
	}

	private EditorOpener() {
		// prevent instantiation - only static method openEditor is to be used
	}
}
