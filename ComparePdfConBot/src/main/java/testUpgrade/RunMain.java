package testUpgrade;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;


import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import net.windward.datasource.DataSourceProvider;
import net.windward.datasource.dom4j.Dom4jDataSource;
import net.windward.env.DataConnectionException;
import net.windward.env.DataSourceException;
import net.windward.env.OutputLimitationException;
import net.windward.format.TemplateParseException;
import net.windward.tags.TagException;
import net.windward.util.LicenseException;
import net.windward.xmlreport.AlreadyProcessedException;
import net.windward.xmlreport.ProcessPdf;
import net.windward.xmlreport.ProcessReport;
import net.windward.xmlreport.ProcessReportAPI;
import net.windward.xmlreport.SetupException;

public class RunMain {

	public static void main(String[] args) throws Exception {


//		// Read the template file.
//        FileInputStream template = new FileInputStream("C:\\Users\\giovanni.bottalico\\Google Drive\\Lavoro\\MIGRAZIONE_WINDWARD\\medioBasso\\sinistri\\valutazione_re - new\\c_out_template\\valutazione_re.docx");
//        
//        FileInputStream data = new FileInputStream("C:\\Users\\giovanni.bottalico\\Google Drive\\Lavoro\\MIGRAZIONE_WINDWARD\\medioBasso\\sinistri\\valutazione_re - new\\test.xml");
//
//		// Create the generated report file.
//        FileOutputStream reportStream = new FileOutputStream("C:\\Users\\giovanni.bottalico\\Google Drive\\Lavoro\\MIGRAZIONE_WINDWARD\\medioBasso\\sinistri\\valutazione_re - new\\new\\valutazione_re.pdf");
//
//        // Pass the 2 streams to the object that will create a PDF report.
//        // For other output types, you create a different object at this step.
//        ProcessReportAPI myReport = new ProcessPdf(data,template, reportStream);
//
//        // Read in the template and prepare it to merge the data
//        myReport.process();
//		// Place all variables in this map. We assign this map to all datasources.
//        Map<String, Object> mapVariables = new HashMap<String, Object>();
//       
//        template.close();
//        reportStream.close();
        testCompareTwoPdfFromStreams();

	}
	
	public static void testCompareTwoPdfFromStreams() throws IOException {
		
		String templateName = "LRM002_EVO";
		
		//String workingDir = "I:\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW\\"+ templateName +"\\";
		String workingDir = "\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW\\"+ templateName +"\\";
        String newPdf = workingDir + "NEW_" + templateName + ".pdf";
        String oldPdf = workingDir + "OLD_" + templateName + ".pdf";
        String mergedPdf = workingDir + "MERGED_" + templateName;
        
        if(new File(newPdf).exists() && new File(oldPdf).exists()) {
        	//Runtime.getRuntime().exec("taskkill /IM AcroRd32.exe");
        	
        	/*
        	int count = 1;
        	while(new File(mergedPdf).exists()){
        		count ++;
        		mergedPdf = mergedPdf.replace("_", "");
        	}
        	*/
        	
        	boolean isEquals = new PdfComparator<CompareResult>(newPdf, oldPdf).compare().writeTo(mergedPdf);
            if (!isEquals) {
                System.out.println("Differences found!");
            }
            
            /*
            
            while(!new File(mergedPdf).exists()) {
            	try {
                	Thread.sleep(1000);
                } catch (InterruptedException e) {
    				e.printStackTrace();
    			}
            }
            
            try {
                Desktop.getDesktop().open(new File(mergedPdf));
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            */
            
        }
        else {
        	System.out.println("Old or new pdf not found!");
        }
    }

}
