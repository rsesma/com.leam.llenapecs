package com.leam.llenapecs;

import java.awt.FileDialog;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
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
	    	String defdir = "";				// default dir: the jar folder
	    	try {
	    		defdir = new File(LlenaPECs.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
	    	} catch (Exception e) {
    			defdir = System.getProperty("user.home");		// if not available, user home directory
    		}
	    	dlg.setDirectory(defdir);
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
				List<String> readonly = new ArrayList<String>();
				for (PDField f : fields) {
					String name = f.getFullyQualifiedName();
                	// ignore APE1, APE2, NOMBRE, DNI, HONOR, COMENT fields
                	if (!name.equalsIgnoreCase("APE1") & !name.equalsIgnoreCase("APE2") & !name.equalsIgnoreCase("NOMBRE") &
                		!name.equalsIgnoreCase("DNI") & !name.equalsIgnoreCase("HONOR") & !name.equalsIgnoreCase("COMENT") &
                		(name.substring(0,1).equalsIgnoreCase("P") | name.substring(0,1).equalsIgnoreCase("M"))) {
                		
                		if (name.indexOf("_") != name.lastIndexOf("_")) name = name.substring(0,name.lastIndexOf("_"));
                		
                		if (name.substring(0,1).equalsIgnoreCase("P")) pnames.add(name);
                		if (name.substring(0,1).equalsIgnoreCase("M")) mnames.add(name);
                		
                		if (f.isReadOnly()) readonly.add(name);
                		
                		String value = "";
                		
                		if (f instanceof PDTextField) {
                			PDTextField e = (PDTextField) f;				// text field: numeric or memo
                			int max = e.getMaxLen();
                			if (max>0) {
                				// for short (numeric) fields, 1234567890 text according to field length
								if (max <= 10) value = "1234567890".substring(0,max);				 
								// for long (memo) fields, repeated "a" until maximum length
								else if (max <=1000) value = String.join("", Collections.nCopies(max, "a"));	
                			}
                		}
                		if (f instanceof PDComboBox) {
                			PDComboBox c = (PDComboBox) f;					// combobox field: closed answer
                			List<String> opts = c.getOptions();
                			value = opts.get(opts.size()-1);
                		}
                		
                		if (value.length()>0) f.setValue(value);			// set the field value (fill)
                	}
				}
				
				// check for fixed fields
				String fixed = "";
				for (String n : "APE1 APE2 NOMBRE DNI HONOR COMENT".split(" ")) {
			    	try {
			    		if (n.equalsIgnoreCase("HONOR")) {
			    			PDCheckBox ch = (PDCheckBox) form.getField(n);
			    			ch.check();
			    		} else {
				    		PDTextField field = (PDTextField) form.getField(n);
				    		if (n.equalsIgnoreCase("APE1")) field.setValue("PRIMER APELLIDO");
				    		if (n.equalsIgnoreCase("APE2")) field.setValue("SEGUNDO APELLIDO");
				    		if (n.equalsIgnoreCase("NOMBRE")) field.setValue("NOMBRE");
				    		if (n.equalsIgnoreCase("DNI")) field.setValue("12345678X");
				    		if (n.equalsIgnoreCase("COMENT")) field.setValue("COMENTARIO");			    			
			    		}
			    	} catch (Exception e) {
			    		fixed = fixed + (fixed.length()>0 ? ";" : "") + n;
		    		}
				}
				// save pdf as a copy
				pdf.save(dir.replace(".pdf","_campos.pdf"));
				pdf.close();
				form = null;
				pdf = null;
				
				// sort p and m names
				Collections.sort(pnames);
				Collections.sort(mnames);
				List<String> names = new ArrayList<String>();
				HashSet<String> dup = new HashSet<String>();
				for (String p : pnames) {
					if (names.contains(p)) dup.add(p);
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
				
				String newline = System.getProperty("line.separator");
				if (lMessage) {
					JOptionPane.showMessageDialog(frame,"Proceso finalizado." + newline + newline +
							"Se han creado los archivos:" + newline +
							dir.replace(".pdf","_campos.pdf") + newline +
							dir.replace(".pdf","_campos.txt"),"Llena PECs",JOptionPane.INFORMATION_MESSAGE);					
				}
				
				if (fixed.length()>0 | !dup.isEmpty() | !readonly.isEmpty()) {
					JOptionPane.showMessageDialog(frame,
							(fixed.length()>0 ? "Falta(n) campo(s) fijo(s): " + fixed + newline + newline : "" ) +
							(!dup.isEmpty() ? "Hay nombres duplicados: " + dup.toString() + newline + newline : "" ) +
							(!readonly.isEmpty() ? "Hay campos desactivados: " + readonly.toString() : "" ),
							"Llena PECs",JOptionPane.WARNING_MESSAGE);
				}
    		} catch (Exception e) {
    			JOptionPane.showMessageDialog(frame,e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    		}
    	}
    	
    	System.exit(0);
    }
}
