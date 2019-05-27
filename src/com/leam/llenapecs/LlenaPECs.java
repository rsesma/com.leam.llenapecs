package com.leam.llenapecs;

import java.awt.FileDialog;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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
    	Boolean lMessage = true;
    	
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
    		if (args.length > 1) lMessage = args[1].equalsIgnoreCase("verbose");
    		else lMessage = false;
    	}
    	
    	if (lContinue) {
    		try {
				// load the pdf & the form
				PDDocument pdf = PDDocument.load(new File(dir));
			    PDAcroForm form = pdf.getDocumentCatalog().getAcroForm();
			    
				// Get field names
				List<PDField> fields = form.getFields();
				List<String> pnames = new ArrayList<String>();
				List<String> mnames = new ArrayList<String>();
				for (PDField f : fields) {
					String name = f.getFullyQualifiedName();
                	// ignore APE1, APE2, NOMBRE, DNI, HONOR, COMENT fields
                	if (!name.equalsIgnoreCase("APE1") & !name.equalsIgnoreCase("APE2") & !name.equalsIgnoreCase("NOMBRE") &
                		!name.equalsIgnoreCase("DNI") & !name.equalsIgnoreCase("HONOR") & !name.equalsIgnoreCase("COMENT") &
                		(name.substring(0,1).equalsIgnoreCase("P") | name.substring(0,1).equalsIgnoreCase("M"))) {
                		
                		if (name.substring(0,1).equalsIgnoreCase("P")) pnames.add(name);
                		if (name.substring(0,1).equalsIgnoreCase("M")) mnames.add(name);
                		
                		String value = "";
                		
                		if (f instanceof PDTextField) {
                			PDTextField e = (PDTextField) f;				// text field: numeric or memo
                			int max = e.getMaxLen();
							if (max <= 10) value = "1234567890".substring(0,max-1);				// for short (numeric) fields, 1234567890 text according to field length 
							else if (max <=1000) value = String.join("", Collections.nCopies(max, "a"));	// for long (memo) fields, repeated "a" until maximum length
                		}
                		if (f instanceof PDComboBox) {
                			PDComboBox c = (PDComboBox) f;					// combobox field: closed answer
                			List<String> opts = c.getOptions();
                			value = opts.get(opts.size()-1);
                		}
                		
                		if (value.length()>0) f.setValue(value);			// set the field value (fill)
                	}
				}
				pdf.save(dir.replace(".pdf","_campos.pdf"));				// save pdf as a copy
				pdf.close();
				form = null;
				pdf = null;
				
				// sort p and m names
				Collections.sort(pnames);
				Collections.sort(mnames);
				List<String> names = new ArrayList<String>();				
				for (String p : pnames) {
					names.add(p);
					int i = Integer.parseInt(p.substring(0,p.indexOf("_")).replace("P",""));
					for (String m : mnames) {
						int j = Integer.parseInt(m.substring(0,m.indexOf("_")).replace("M",""));
						if (i == j) names.add(m);
					}
				}
				// write field names to txt
				Path fdata = Paths.get(dir.replace(".pdf","_campos.txt"));
				Files.write(fdata, names, Charset.forName("UTF-8"));
				
				if (lMessage) {
					String newline = System.getProperty("line.separator");
					JOptionPane.showMessageDialog(frame,"Proceso finalizado." + newline + newline +
							"Se han creado los archivos:" + newline +
							dir.replace(".pdf","_campos.pdf") + newline +
							dir.replace(".pdf","_campos.txt"),"Llena PECs",JOptionPane.INFORMATION_MESSAGE);					
				}
    		} catch (Exception e) {
    			JOptionPane.showMessageDialog(frame,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    		}
    	}
    	
    	System.exit(0);
    }
}
