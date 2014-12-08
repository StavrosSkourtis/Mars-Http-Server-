package mars.http;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Phenom
 */
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import mars.utils.Config;
import mars.utils.Logger;

public class ClientThread extends Thread{
    
    private String root;
    private Socket client;
    public ClientThread(Socket client,String root){
        this.root = root;
        this.client = client;
    }
    
                
    public void run(){
        
        try{
            /*
                If the clints ip is black listed 
                we will close the connection
            */
            for(String ip : Config.BLACK_LIST){
                if(client.getInetAddress().getHostAddress().equals(ip)){
                    client.close();
                    return;
                }

            }
        
        
            // get socket i/o
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            boolean connection;
            
            do{
                // clear the list so we can handle new a request
               
                
                int length;
                
                //Create a new HTTPRequest (an object that represents it , not an actual http request) and pass it the socket input stream;
                HTTPRequest request = new HTTPRequest(root);
                request.create(in);
                              
                

                // it parses the request and send back the response
                HTTPRequestHandler requestProcess = new HTTPRequestHandler(request,out);
                // returns true if keep alive , false if close or not exists
                connection = requestProcess.process(client.getInetAddress().getHostAddress(),client.getPort());
                
            }while(connection);
            
            // close i/o and socket
            in.close();
            out.close();
            client.close();
        }catch(IOException e){   
            
        }
    }
    
   
}
