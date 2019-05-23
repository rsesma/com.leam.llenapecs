package com.leam.llenapecs;

import java.awt.FileDialog;
import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDComboBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;

public class LlenaPECs {
	public static String dir;
	
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	
    	Boolean lContinue = true;
        JFrame frame = new JFrame("JFileChooser dialog window");        			// JFrame for the dialogs
    	
    	if (args.length == 0) {
	    	FileDialog dlg = new FileDialog(frame, "Indicar la PEC", FileDialog.LOAD);
	    	dlg.setDirectory(System.getProperty("user.home"));			// default dir
	    	dlg.setFile("*.pdf");										// filter: PDF files only
	    	dlg.setVisible(true);
	    	String filename = dlg.getFile();
	    	if (filename == null) lContinue = false; 
	    	else dir = dlg.getDirectory() + dlg.getFile();
    	} else {
    		dir = args[0];
    	}
    	
    	if (lContinue) {
    		try {
				// load the pdf & the form
				PDDocument pdf = PDDocument.load(new File(dir));
			    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
			    
				// Get field names
				List<PDField> fields = form.getFields();
				for (PDField f : fields) {
					String name = f.getFullyQualifiedName();
                	// ignore APE1, APE2, NOMBRE, DNI, HONOR, COMENT fields
                	if (!name.equalsIgnoreCase("APE1") & !name.equalsIgnoreCase("APE2") & !name.equalsIgnoreCase("NOMBRE") &
                		!name.equalsIgnoreCase("DNI") & !name.equalsIgnoreCase("HONOR") & !name.equalsIgnoreCase("COMENT")) {
                		if (f instanceof PDTextField) {
                			System.out.println(name + ": text");                			
                		}
                		else if (f instanceof PDComboBox) {
                			System.out.println(name + ": combo");                			
                		}
                		else {
                			System.out.println(name + ": unknown");
                		}
                	}
				}
				
    		} catch (Exception e) {
    			JOptionPane.showMessageDialog(frame,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    		}
    	}
    	
    	System.exit(0);
    }
}
