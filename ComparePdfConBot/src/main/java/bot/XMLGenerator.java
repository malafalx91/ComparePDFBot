package bot;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class XMLGenerator {
	
	private PathsManager pathsManager = null;
	private String sourceXmlAsString = null;
	private String standardMatrixCode = "<matrix x=\"525\" y=\"60\" x1=\"22\" y1=\"20\" write=\"all\" size=\"6\" sizefooter=\"5\" numpage=\"true\" text=\"022-10554885-1-1112-ITASDM-POLIZ-10001-S\">022000000010554885000001001112ITASDMPOLIZ10001s</matrix>";
	
	
	//for testing purposes
	public static void main(String[] args) {
		PathsManager pathsManager = null;
		try {
			pathsManager = new PathsManager();
			pathsManager.setBaseWorkingDir("\\\\clufssvi.gruppoitas.local\\discoi\\LAVORO\\Fincons-Danni\\Windward_Stampe_NEW");
			pathsManager.setWatchDir("\\\\assvi21.gruppoitas.local\\PDFReportServer\\eco_pdfreport\\template\\vitaTst");
			pathsManager.setTemplateName("LRM001_EVO");
			
			XMLGenerator xmlManager = new XMLGenerator(pathsManager);
			//the EXACT order of methods invocation:
			xmlManager.loadSourceXml();
			xmlManager.generateRtfXMLFile();
			xmlManager.generateDocxXMLFile();
			System.out.println(xmlManager.getDocxXml());
			System.out.println(xmlManager.getRtfXml());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(pathsManager != null) pathsManager.closeConnection();
		}
	}
	
	
	
	public XMLGenerator(PathsManager pathsManager) {
	    this.pathsManager = pathsManager;
	}
	
	/**
	 * Loads the source xml into a string and keeps it for further processing
	 * @throws SQLException 
	 * @throws IOException
	 */
	public void loadSourceXml() throws SQLException, IOException {
		//byte[] bytes = Files.readAllBytes(Paths.get(pathsManager.getSourceXmlFilePath()));
		//se manca xml e bisogna metterlo manualmente cambiare path e commentare la precedente riga 
		byte[] bytes = Files.readAllBytes(Paths.get("D:\\eco_pdfreport\\se_manca_xml\\contratto_6a0750.xml"));
		sourceXmlAsString = new String(bytes, Charset.forName("UTF-8"));
	}
	
	public void generateRtfXMLFile() throws IOException {
		String rtfXmlAsString = generateRtfXml(sourceXmlAsString);
		InputStream rtfXmlIS = new ByteArrayInputStream(rtfXmlAsString.getBytes(StandardCharsets.UTF_8));
		Path rtfXmlFilePath = new File(pathsManager.getRtfXMLFilePath()).toPath();
		Files.copy(rtfXmlIS, rtfXmlFilePath, REPLACE_EXISTING);
	}
	
	public void generateDocxXMLFile() throws IOException {
		String docxXmlAsString = generateDocxXml(sourceXmlAsString);
		InputStream docxXmlIS = new ByteArrayInputStream(docxXmlAsString.getBytes(StandardCharsets.UTF_8));
		Path docxXmlFilePath = new File(pathsManager.getDocxXMLFilePath()).toPath();
		if(!docxXmlFilePath.toFile().exists()) {
			Files.copy(docxXmlIS, docxXmlFilePath);
		}
	}
	
	/**
	 * @return (re)reads the xml from the xml file
	 * @throws IOException 
	 */
	public String getDocxXml() throws IOException {
		return readFileAsString(pathsManager.getDocxXMLFilePath());
	}
	
	/**
	 * @return (re)reads the xml from the xml file
	 * @throws IOException
	 */
	public String getRtfXml() throws IOException {
		return readFileAsString(pathsManager.getRtfXMLFilePath());
	}
	

	
	
	
	
	
	/***************************************** PRIVATE METHODS *******************************************/
	
	
	
	private String generateRtfXml(String sourceXml) {
		//no replaces for now
		return sourceXml;
	}
	
	private String generateDocxXml(String sourceXml) {
		return addMatrixCode(sourceXml.replaceAll(".rtf", ".docx"));
	}
	
	private String addMatrixCode(String sourceXml) {
		if(sourceXml.contains("<document>")){
			//after document tag
			return sourceXml.replaceFirst("<document>", "<document>" + standardMatrixCode);
		}
		else if(sourceXml.contains("<loghi>")) {
			//on top of loghi tag
			return sourceXml.replaceFirst("<loghi>", standardMatrixCode + "<loghi>");
		}
		else {
			//before last tag opening
			int index = sourceXml.lastIndexOf("</");
			return sourceXml.substring(0, index) + standardMatrixCode + sourceXml.substring(index, sourceXml.length());
			//after first tag close (whatever it is)
			//return sourceXml.replaceFirst(">", ">" + standardMatrixCode);
		}
	}
	
	private String readFileAsString(String filePath) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(filePath));
		return new String(bytes, Charset.forName("UTF-8"));
	}

}
