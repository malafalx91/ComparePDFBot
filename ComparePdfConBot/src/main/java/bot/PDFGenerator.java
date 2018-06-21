package bot;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;

public class PDFGenerator {
	
	private PathsManager pathsManager;
	private XMLGenerator xmlManager;
	private int mergeCount = 0;
	
	public PDFGenerator(PathsManager pathsManager, XMLGenerator xmlManager) {
		this.pathsManager = pathsManager;
		this.xmlManager = xmlManager;
	}
	
	
	@SuppressWarnings("deprecation")
	public File generatePdfFromRtf() throws ClientProtocolException, IOException {
		Path rtfTemplatePdfFilePath = new File(pathsManager.getRtfTemplatePdfFilePath()).toPath();
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httPost = new HttpPost(pathsManager.getOldWebappUrl());
		
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("template", "vitaTst\\" + pathsManager.getTemplateName() + ".rtf"));
        nvps.add(new BasicNameValuePair("data", xmlManager.getRtfXml()));
        
		httPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = httpclient.execute(httPost);
        HttpEntity entity = response.getEntity();
        
        Files.copy(entity.getContent(), rtfTemplatePdfFilePath, REPLACE_EXISTING);

        System.out.println(rtfTemplatePdfFilePath + " generated!");
        
        if (entity != null) {
        	EntityUtils.consume(entity);
        }
        
        httpclient.close();

        return rtfTemplatePdfFilePath.toFile();
	}
	
	@SuppressWarnings("deprecation")
	public File generatePdfFromDocx() throws ClientProtocolException, IOException {
		Path docxTemplatePdfFilePath = new File(pathsManager.getDocxTemplatePdfFilePath()).toPath();
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httPost = new HttpPost(pathsManager.getNewWebappUrl());
		
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("template", "vitaTst\\" + pathsManager.getTemplateName() + ".docx"));
        nvps.add(new BasicNameValuePair("data", xmlManager.getDocxXml()));
        
		httPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = httpclient.execute(httPost);
        HttpEntity entity = response.getEntity();
        
        Files.copy(entity.getContent(), docxTemplatePdfFilePath, REPLACE_EXISTING);
        
        if (entity != null) {
        	EntityUtils.consume(entity);
        }
        
        System.out.println(docxTemplatePdfFilePath + " generated!");
        
        //generate the DOCX_INITIAL_<template_name>.docx only once (the first time)
        File pfdInitialVersion = new File(pathsManager.getDocxInitialTemplatePdfFilePath());
        if(!pfdInitialVersion.exists()){
        	Files.copy(docxTemplatePdfFilePath, pfdInitialVersion.toPath(), REPLACE_EXISTING);
        	System.out.println(pfdInitialVersion + " generated!");
        }
        
        httpclient.close();

        return docxTemplatePdfFilePath.toFile();
	}
	
	public File generateMergedPdf(File rtfPdf, File docxPdf) throws IOException {
        String mergedPdf = pathsManager.getMergedPdfFilePath(++mergeCount);
        String mergedInitialPdf = pathsManager.getMergedInitialPdfFilePath();
        
		if(rtfPdf.exists() && docxPdf.exists()) {
			System.out.println("Starting to merge pdfs");
        	boolean isEquals = new PdfComparator<CompareResult>(docxPdf, rtfPdf).compare().writeTo(mergedPdf.replace(".pdf", ""));
        	System.out.println(mergedPdf + " generated!");
            if (!isEquals) {
                System.out.println("Differences found!");
            }
            
            //generate the MERGED_INITIAL_<template_name>.pdf only once (the first time)
	        File pfdMergedInitialVersion = new File(mergedInitialPdf);
	        if(!pfdMergedInitialVersion.exists()){
	        	Files.copy(new File(mergedPdf).toPath(), pfdMergedInitialVersion.toPath(), REPLACE_EXISTING);
	        	System.out.println(pfdMergedInitialVersion + " generated!");
	        }
	        
            return new File(mergedPdf);
        }
        else {
        	System.out.println("Old or new pdf not found!");
        	return null;
        }
	}
	
	public void reset() {
		mergeCount = 0;
	}

}
