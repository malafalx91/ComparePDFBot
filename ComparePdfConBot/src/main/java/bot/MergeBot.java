package bot;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.SQLException;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.internal.PackagePropertiesPart;
import org.apache.xmlbeans.XmlException;

public class MergeBot {
	
	private static String myOfficeName = "Francesco Zingaro";
	private static String myStopFlag = "ZZ_Fra.txt";
	private static boolean isWorkingOnLocale = true;
	//	
	private static File rtfPdf = null;
	private static XMLGenerator xmlGenerator;
	private static PathsManager pathsManager;
	private static PDFGenerator pdfGenerator;
	private static String lastProcessedTemplateName = "";
	private static String lastMergedPdfFileName = "";
	private static boolean canContinue = true;
	
	
	private static void config() throws ClassNotFoundException, SQLException {
		pathsManager = new PathsManager();
		
		if(isWorkingOnLocale) {
			//config to work on locale
			pathsManager.setBaseWorkingDir("D:\\eco_pdfreport\\Windward_Stampe_NEW");
			pathsManager.setWatchDir("D:\\eco_pdfreport\\template\\vitaTst");
			pathsManager.setOldWebappUrl("http://pdfgentest.gruppoitas.local:8080/PDFReportServerVitaTst/generatepdfreport");
			pathsManager.setNewWebappUrl("http://localhost:8080/PDFReportServer/generatepdfreport");
		}
		else {
			//config to work on server
			pathsManager.setBaseWorkingDir("\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW");
			pathsManager.setWatchDir("\\\\assvi21.gruppoitas.local\\PDFReportServer\\eco_pdfreport\\template\\vitaTst");
			pathsManager.setOldWebappUrl("http://pdfgentest.gruppoitas.local:8080/PDFReportServerVitaTst/generatepdfreport");
			pathsManager.setNewWebappUrl("http://assvi21.gruppoitas.local:8080/PDFReportServer/generatepdfreport");
		}
		
		xmlGenerator = new XMLGenerator(pathsManager);
		pdfGenerator = new PDFGenerator(pathsManager, xmlGenerator);
	}
	
	
	public static void main(String[] args) {
		try {
			config();
	        WatchService watcher = FileSystems.getDefault().newWatchService();
	        Path dir = new File(pathsManager.getWatchDir()).toPath();
	        dir.register(watcher, ENTRY_MODIFY);
	        
	        System.out.println("Waiting for docx modification in " + pathsManager.getWatchDir());
	        while(canContinue) {

	            // wait for key to be signaled
	            WatchKey key = watcher.take();
	            
	            // Prevent receiving two separate ENTRY_MODIFY events: file modified and timestamp updated.
	            // Instead, receive one ENTRY_MODIFY event with two counts
	            Thread.sleep(1000);

	            for (WatchEvent<?> event: key.pollEvents()) {
	                WatchEvent.Kind<?> kind = event.kind();
	                if (kind == ENTRY_MODIFY) {
	                	
	                	@SuppressWarnings("unchecked")
						WatchEvent<Path> ev = (WatchEvent<Path>)event;
	                    Path modifiedFile = ev.context();
	                    //Path child = dir.resolve(filename);
	                    
	                    if(isStopFlag(modifiedFile)) {
	                    	System.out.println("\nStop flag received");
	                    	canContinue = false;
	                    	break;
	                    }
	                    
	                    if(isAcceptedTemplate(modifiedFile)) {
	                    	onTemplateChange(modifiedFile);
	                    }
	                    else {
	                    	//System.out.println("Skypped modified file " + modifiedFile);
	                    }
	                }
	            }

	            // Reset the key. This step is critical if you want to receive further watch events.
	            // If the key is no longer valid, the directory is inaccessible so exit the loop.
	            boolean valid = key.reset();
	            if (!valid) {
	                break;
	            }
	        }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(pathsManager != null) pathsManager.closeConnection();
			System.out.println("Bot has been terminated");
		}
	}
	

	/**
	 * Logic to be executed at each template change
	 * @throws SQLException
	 */
	private static void onTemplateChange(Path templateName) throws IOException, SQLException {
		System.out.println("\n" + templateName + " has been modified.");
    	
    	if(isWorkingOnNewTemplate(templateName)) {
    		System.out.println("Working on a new template");
    		lastProcessedTemplateName = templateName.toString();
    		lastMergedPdfFileName = "";
    		pathsManager.setTemplateName(templateName.toString().replace(".docx", ""));
    		
    		//backup an original copy of the template we are modifying 
    		backupOriginalDocx();
    		
    		//generates the xml file for rtf and docx
    		xmlGenerator.loadSourceXml();
			xmlGenerator.generateRtfXMLFile();
			xmlGenerator.generateDocxXMLFile();
			
			//generates the pdf from the RTF
			pdfGenerator.reset();
			rtfPdf = pdfGenerator.generatePdfFromRtf();
    	}
    	
    	//copies the last modified docx template into the working dir
    	copyModifiedDocx();
    	
    	//generates the pdf from the DOCX
    	File docxPdf = pdfGenerator.generatePdfFromDocx();
    	
    	//merges the 2 pdfs
    	File mergedPdf = pdfGenerator.generateMergedPdf(rtfPdf, docxPdf);
    	
    	//tries to open the merged pdf in windows
    	openFileInSumatraPDF(mergedPdf);
    	
    	//tries to delete the last merged pdf file
        deleteLastMergedPdfFile(mergedPdf);
	}
	
	
	
	
	private static boolean isStopFlag(Path modifiedFile) {
		return modifiedFile.toString().equalsIgnoreCase(myStopFlag);
	}
	
	private static boolean isAcceptedTemplate(Path modifiedFile) throws IOException, OpenXML4JException, XmlException {
		return isAcceptedDocx(modifiedFile) && isModifiedByMe(modifiedFile);
	}
	
	private static boolean isAcceptedDocx(Path modifiedFile) {
		return !modifiedFile.toString().startsWith("~$") && modifiedFile.toString().endsWith(".docx");
	}
	
	private static boolean isModifiedByMe(Path templateName) throws IOException, OpenXML4JException, XmlException {
		if(!isWorkingOnLocale) {
			OPCPackage pkg = OPCPackage.open(new File(pathsManager.getWatchDir() + "\\" + templateName.toString()));
			POIXMLProperties props = new POIXMLProperties(pkg);
			PackagePropertiesPart ppropsPart = props.getCoreProperties().getUnderlyingProperties();
			return ppropsPart.getLastModifiedByProperty().getValue().equalsIgnoreCase(myOfficeName);
		}
		else {
			return true;
		}
	}
	
	@SuppressWarnings("unused")
	private static void openFileInWindows(File mergedPdf) {
		System.out.println("Opening in windows" + mergedPdf);
        try {
            Desktop.getDesktop().open(mergedPdf);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private static void openFileInSumatraPDF(File mergedPdf) throws IOException {
		System.out.println("Opening in Sumatra PDF " + mergedPdf);
		String desktopPath = System.getProperty("user.home") + "/Desktop";
		String sumatraPdfExePath = desktopPath + "/SumatraPDF.exe";
		new ProcessBuilder(sumatraPdfExePath, mergedPdf.toString()).start();
	}
	
	private static void deleteLastMergedPdfFile(File mergedPdf) {
		if(lastMergedPdfFileName != null && !lastMergedPdfFileName.equals("")) {
        	System.out.println("Deleting old merged file " + lastMergedPdfFileName);
        	if(new File(lastMergedPdfFileName).delete()) {
        		System.out.println(lastMergedPdfFileName + " deleted successfully");
        	}
        	else {
        		System.out.println("Unable to delete " + lastMergedPdfFileName + ". Maybe it's opened.");
        	}
        }
        lastMergedPdfFileName = mergedPdf.toString();
	}

	private static boolean isWorkingOnNewTemplate(Path templateName) {
		return !lastProcessedTemplateName.equals(templateName.toString());
	}
	
	private static void backupOriginalDocx() throws IOException {
		Files.copy(new File(pathsManager.getSourceDocxTemplateFilePath()).toPath(), new File(pathsManager.getDocxOriginalTemplateFilePath()).toPath(), REPLACE_EXISTING);
	}

	private static void copyModifiedDocx() throws IOException {
		Files.copy(new File(pathsManager.getSourceDocxTemplateFilePath()).toPath(), new File(pathsManager.getDocxModifiedTemplateFilePath()).toPath(), REPLACE_EXISTING);
	}

}
