package com.phishcave.phishpic;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


import android.util.Log;

public class HttpPostUploader
{
    private String Tag = "Phishpic";
    HttpURLConnection conn;

    public HttpPostUploader(byte[] data, String filename, String urlString)
    {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            //Content-Disposition: form-data; name="upload[file]"; filename="220px-Nyan_cat_250px_frame.PNG"
            dos.writeBytes("Content-Disposition: post-data; name=upload[file];filename="
                            + filename + lineEnd);
            dos.writeBytes(lineEnd);

            dos.write(data, 0, data.length);

            // send multipart form data necessary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            Log.e(Tag, "Error: " + ex.getMessage(), ex);
        }
        catch (IOException ioe) {
            Log.e(Tag, "Error: " + ioe.getMessage(), ioe);
        }

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = null;
        try {
            sp = spf.newSAXParser();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // Get the XMLReader of the SAXParser we created.
        XMLReader xr = null;
        try {
            xr = sp.getXMLReader();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        // Create a new ContentHandler and apply it to the XML-Reader
        xr.setContentHandler(new DefaultHandler());

        // Parse the xml-data from our URL.
        try {
            xr.parse(new InputSource(conn.getInputStream()));
            //xr.parse(new InputSource(new java.io.FileInputStream(new java.io.File("login.xml"))));
        } catch (MalformedURLException e) {
            Log.d(Tag, "NetDisconnected");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(Tag, "NetDisconnected");
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }
}
