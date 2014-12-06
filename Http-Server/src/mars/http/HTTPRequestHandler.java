package mars.http;

import mars.http.HTTPStatus;
import mars.http.HTTPServer;
import java.io.*;
import java.util.ArrayList;
import static mars.http.HTTPStatus.*;
import mars.utils.Config;
import mars.utils.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Stavros Skourtis
 */
public class HTTPRequestHandler {
    
    private final HTTPRequest request;
    private HTTPResponse response;
    private final DataOutputStream outStream;
    
    /*
        Constructor
    */
    public HTTPRequestHandler(HTTPRequest request,DataOutputStream outStream){
        this.request = request;
        this.outStream = outStream;
        response = new HTTPResponse();
    }
    
    public boolean process(String ip,int port) throws IOException{
        runMethod();
        response.send(outStream);
        // adding records to the log file (log.txt)
        Logger.addRecord(ip, port, request.method+" "+request.url , response.getStatusCode());
        
        return request.getHeaderField("connection")!=null && !request.getHeaderField("connection").equalsIgnoreCase("keep-alive");
    }
    
    
    private void runMethod() throws IOException{
        /*
            If protocol is not HTTP/1.1 or HTTP/1.0 is is not suported
        */
        if(! (request.protocolVersion.equals("HTTP/1.1") || request.protocolVersion.equals("HTTP/1.0") )){
            response = code505();
            return;
        }
        
        /*
            The request MUST contain a host header field
        */
        if(request.getHeader("host")==null){
            response = code400();
            return;
        }
        
        /*
            If true the project is modified
        */
        if (request.getHeader("if-un-modified-since")!=null && request.urlFile.exists() && !request.getHeader("if-un-modified-since").getCondition()){ 
            response = code412();
            return;
        }

        /*
            If true the project is not modified , so 304 status code is send back
        */
        if(request.getHeader("if-modified-since")!=null && request.urlFile.exists() && !request.getHeader("if-modified-since").getCondition()){
            response = code304();
            return;
        }
        
        
        /* 
                Check http method and return repsonse
                if the  requested Method is not valid
                a   400    response   is  sent   back
                                                            */  
        if(request.method.equalsIgnoreCase("GET")){
            if(Config.GET)
                get();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("POST")){
            if(Config.POST)
                post();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("HEAD")){
            if(Config.HEAD)
                head();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("PUT")){
            if(Config.PUT)
                put();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("DELETE")){
            if(Config.DELETE)
                delete();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("CONNECT")){
            if(Config.CONNECT)
                connect();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("TRACE")){
            if(Config.TRACE)
                trace();
            else
                response = code405();
        }else if(request.method.equalsIgnoreCase("OPTIONS") && Config.OPTIONS){
            if(Config.OPTIONS)
                options();
            else
                response = code405();
        }else{
            response = code400();
        };
        
    } 
    
    private void get() throws IOException{
        boolean exists = false;
       
        if(request.urlFile.exists()){
            if(request.urlFile.isDirectory()){
                String tempPath = request.urlFile.getPath();
                for (String defaultPage : Config.DEFAULT_PAGES) {    
                    if (tempPath.endsWith("/")) {
                        request.urlFile = new File(tempPath+defaultPage);
                    }else {
                        request.urlFile = new File(tempPath+"/"+defaultPage);
                    }
                    if(request.urlFile.exists()){
                        exists = true;
                        break;
                    }
                }
            }else{     
                exists = true;
            }
        }
        
        if(exists){
            byte[] body = ServerUtils.getBinaryFile(request.urlFile);
            
            response.setStatusCode(HTTP_VERSION+" 200 OK");
            response.addHeader("connection", request.getHeaderField("connection"));
            response.addHeader("date", ServerUtils.getServerTime());
            response.addHeader("server",SERVER_NAME);
            response.addHeader("last-modified", String.valueOf(request.urlFile.lastModified()));
            response.addHeader("content-length", String.valueOf(body.length));
            response.addBody(body);
        }else
            response = code404();
    }
    
    
    private void post(){
        
    }
    
    private void head(){
        
    }
    
    private void put() throws IOException{
        System.out.println("here");
        response = code204();
        FileOutputStream writer = new FileOutputStream(request.urlFile);
        writer.write(request.body);
        writer.close();
        System.out.println("here");
    }
    
    private void delete() throws IOException{
        boolean exists = false;
       
        if(request.urlFile.exists()){
            if(request.urlFile.isDirectory()){
                String tempPath = request.urlFile.getPath();
                for (String defaultPage : Config.DEFAULT_PAGES) {    
                    if (tempPath.endsWith("/")) {
                        request.urlFile = new File(tempPath+defaultPage);
                    }else {
                        request.urlFile = new File(tempPath+"/"+defaultPage);
                    }
                    if(request.urlFile.exists()){
                        exists = true;
                        break;
                    }
                }
            }else{     
                exists = true;
            }
        }
        
        if(exists){
            if(request.urlFile.delete()){
                response = code204();
            }else{
                System.out.println("Error while deleting it");
            }
        }else
            response = code404();
    }
    
    private void connect() throws IOException{
        response.setStatusCode(HTTP_VERSION+" 200 Connection established");
        response.addHeader("date", ServerUtils.getServerTime());
        response.addHeader("server", SERVER_NAME);
    }
    
    private void trace()throws IOException{        
        response.setStatusCode(HTTP_VERSION+" 200 OK");
        response.addHeader("date", ServerUtils.getServerTime());
        response.addHeader("server", SERVER_NAME);
        response.addHeader("connection",request.getHeaderField("connection"));
        response.addHeader("content-type","message/http");
        response.addHeader("content-length" ,String.valueOf(request.getRequestString().getBytes().length));
        response.addBody(request.getRequestString().getBytes());
    }
    
    private void options() throws IOException{   
        String allow= "";
        
        if(Config.GET)
            allow+="GET";
        else if(Config.POST)
            allow+=",POST";
        else if(Config.HEAD)
            allow+=",HEAD";
        else if(Config.TRACE)
            allow+=",TRACE";
        else if(Config.OPTIONS)
            allow+=",OPTIONS";
        else if(Config.DELETE)
            allow+=",DELETE";
        else if(Config.PUT)
            allow+=",PUT";
        else if(Config.CONNECT)
            allow+=",CONNECT";
            
        
        response.setStatusCode(HTTP_VERSION+" 200 OK");
        response.addHeader("date", ServerUtils.getServerTime());
        response.addHeader("server",SERVER_NAME);
        response.addHeader("allow", allow);
        response.addHeader("Content-Type","httpd/unix-directory");
    }    
}
