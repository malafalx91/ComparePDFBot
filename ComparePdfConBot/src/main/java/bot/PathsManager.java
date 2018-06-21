package bot;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PathsManager {
	
	//base working dir without the final "\", e.g. "\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW";
	private String baseWorkingDir;
	//template name WITHOUT suffix, e.g. RICEVUTA_MODULO_INPS (and not RICEVUTA_MODULO_INPS.docx)
	private String templateName;
	//
	private String newWebappUrl;
	private String oldWebappUrl;
	private String watchDir;
	//
	private Connection connection;
	private static boolean isTest = false;
	//
	private String pdfgen02 = "pdfgen02.gruppoitas.local";
	private String pdfgen03 = "pdfgen03.gruppoitas.local";
	
	
	//for testing purposes
	public static void main(String[] args) {
		//avoids the coonnection to the DB
		isTest = true;
		try {
			String desktopPath = System.getProperty("user.home") + "/Desktop";
			String sumatraPdfExePath = desktopPath + "/SumatraPDF.exe";
			new ProcessBuilder(sumatraPdfExePath, "\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW\\modulo_gestione_rendimenti_prospetto\\MERGED_2_modulo_gestione_rendimenti_prospetto.pdf").start();
			
			PathsManager pathsManager = new PathsManager();
			pathsManager.setBaseWorkingDir("\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW");
			pathsManager.setWatchDir("\\\\assvi21.gruppoitas.local\\PDFReportServer\\eco_pdfreport\\template\\vitaTst");
			pathsManager.setTemplateName("RICEVUTA_MODULO_INPS");
			
			System.out.println(pathsManager.getBaseWorkingDir());
			System.out.println(pathsManager.getTemplateName());
			System.out.println(pathsManager.getWatchDir());
			System.out.println(pathsManager.getSourceDocxTemplateFilePath());
			System.out.println(pathsManager.getTemplateWorkingDir());
			System.out.println(pathsManager.getRtfXMLFilePath());
			System.out.println(pathsManager.getDocxXMLFilePath());
			System.out.println(pathsManager.getRtfTemplatePdfFilePath());
			System.out.println(pathsManager.getDocxTemplatePdfFilePath());
			System.out.println(pathsManager.getDocxInitialTemplatePdfFilePath());
			System.out.println(pathsManager.getDocxOriginalTemplateFilePath());
			System.out.println(pathsManager.getMergedPdfFilePath(1));
			System.out.println(pathsManager.getMergedInitialPdfFilePath());
			//System.out.println(pathsManager.());
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	public PathsManager() throws ClassNotFoundException, SQLException {
		if(!isTest) {
			Class.forName("oracle.jdbc.OracleDriver");
			System.out.println("Oracle JDBC driver loaded successfully");
	        connection = DriverManager.getConnection("jdbc:oracle:thin:u_gdoct/u_gdoct@//itasprod-scan3.gruppoitas.local:1521/prod.gruppoitas.local");
	        System.out.println("Connect with database itasprod-scan3.gruppoitas.local");
		}
	}
	
	
	
	
	
	/*********************** SOURCE RELATED PATHS ******************************************************************/
	
	
	public String getWatchDir() {
		return watchDir;
	}

	public void setWatchDir(String watchDir) {
		this.watchDir = watchDir;
	}
	
	public String getSourceDocxTemplateFilePath() {
		return watchDir + "\\" + templateName + ".docx";
	}
	
	/**
	 * @return the path of the XML file on pdfgen02.gruppoitas.local or pdfgen03.gruppoitas.local
	 * E.g. \\pdfgen03.gruppoitas.local\vitaPrd\ricevuta_modulo_inps.xml
	 * @throws SQLException 
	 */
	public String getSourceXmlFilePath() throws SQLException {
        Statement stmt = null;
        String sourceXmlFilePath = null;
		try {
			stmt = connection.createStatement();
			String select = "SELECT * FROM LOG_TEMPLATE WHERE RTF_NAME = '" + getTemplateName().toUpperCase() + "'";
	        ResultSet rows = stmt.executeQuery(select);
	        //only one expected
	        while (rows.next()) {
	            String dbPathXml = rows.getString("PATH_XML");
	            sourceXmlFilePath = resolveDbPathXml(dbPathXml);
	        }
	        rows.close();
		} finally {
		    if (stmt != null) {
				stmt.close();
		    }
		}
		
		return sourceXmlFilePath;
	}
	
	private String resolveDbPathXml(String dbXathXml) {
		String[] params = dbXathXml.split("-");
		String server = params[0];
		String absXmlFilePath = params[1];
		
		String xmlFileName = absXmlFilePath.substring(absXmlFilePath.lastIndexOf("\\") + 1, absXmlFilePath.length());
		String xmlFilePath = null;
		
		if(isLoadBalancerServer(server)) {
			//the server is the load balancer: resolving it as pdfgen02 or pdfgen03
			String xmlFilePath1 = "\\\\" + pdfgen02 + "\\vitaPrd\\" + xmlFileName;
			String xmlFilePath2 = "\\\\" + pdfgen03 + "\\logXml\\vitaPrd\\" + xmlFileName;
			if(new File(xmlFilePath1).exists()) {
				xmlFilePath = xmlFilePath1;
			}
			else if(new File(xmlFilePath2).exists()) {
				xmlFilePath = xmlFilePath2;
			}
			else {
				xmlFilePath = "";
			}
		}
		else if(server.equalsIgnoreCase(pdfgen02)) {
			xmlFilePath = "\\\\" + server + "\\vitaPrd\\" + xmlFileName;
		}
		else if(server.equalsIgnoreCase(pdfgen03)) {
			xmlFilePath = "\\\\" + server + "\\logXml\\vitaPrd\\" + xmlFileName;
		}
		
		return xmlFilePath;
	}
	
	private boolean isLoadBalancerServer(String server) {
		return !server.equalsIgnoreCase(pdfgen02) && !server.equalsIgnoreCase(pdfgen03);
	}

	public void closeConnection() {
		try {
			if(connection != null) {
				connection.close();
				System.out.println("Connecton successfully closed");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	/*********************** DESTINATION RELATED PATHS ******************************************************************/
	
	
	/**
	 * @param baseWorkingDir
	 *  base working dir without the final "\", e.g. "\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW";
	 */
	public void setBaseWorkingDir(String baseWorkingDir) {
		this.baseWorkingDir = baseWorkingDir;
	}
	
	public String getBaseWorkingDir() {
		return baseWorkingDir;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
		if(!new File(getTemplateWorkingDir()).exists()) {
        	new File(getTemplateWorkingDir().toUpperCase()).mkdirs();
        }
	}
	
	public String getTemplateWorkingDir() {
		return baseWorkingDir + "\\" + templateName;
	}
	
	public String getRtfXMLFilePath() {
		return getTemplateWorkingDir() + "\\RTF_" + templateName + ".xml";
	}

	public String getDocxXMLFilePath() {
		return getTemplateWorkingDir() + "\\DOCX_" + templateName + ".xml";
	}
	
	public String getRtfTemplatePdfFilePath() {
		return getTemplateWorkingDir() + "\\RTF_" + templateName + ".pdf";
	}
	
	public String getDocxTemplatePdfFilePath() {
		return getTemplateWorkingDir() + "\\DOCX_" + templateName + ".pdf";
	}
	
	public String getDocxInitialTemplatePdfFilePath() {
		return getTemplateWorkingDir() + "\\DOCX_INITIAL_" + templateName + ".pdf";
	}
	
	public String getDocxOriginalTemplateFilePath() {
		return getTemplateWorkingDir() + "\\ORIG_" + templateName + ".docx";
	}
	
	public String getDocxModifiedTemplateFilePath() {
		return getTemplateWorkingDir() + "\\MOD_" + templateName + ".docx";
	}
	
	public String getMergedPdfFilePath(int count) {
		return getTemplateWorkingDir() + "\\MERGED_" + count + "_" + templateName + ".pdf";
	}
	
	public String getMergedInitialPdfFilePath() {
		return getTemplateWorkingDir() + "\\MERGED_INITIAL_" + templateName + ".pdf";
	}
	


	
	
	
	
	/*********************** WEBAPPS RELATED URIS ******************************************************************/
	
	
	public String getNewWebappUrl() {
		return newWebappUrl;
	}
	
	public void setNewWebappUrl(String newWebappUrl) {
		this.newWebappUrl = newWebappUrl;
	}
	
	public String getOldWebappUrl() {
		return oldWebappUrl;
	}

	public void setOldWebappUrl(String oldWebappUrl) {
		this.oldWebappUrl = oldWebappUrl;
	}

	
	
	
}
