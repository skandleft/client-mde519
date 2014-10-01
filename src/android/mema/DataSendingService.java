package android.mema;

import android.app.Service;
import android.content.Context;
import static android.content.Context.MODE_APPEND;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.MODE_WORLD_READABLE;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class DataSendingService extends Service {

  private FileOutputStream writer = null;
  private String server;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
      Boolean net=isNetworkAvailable();
      if(net)
      {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();

        String username = prefs.getString("username", "");
        String age = prefs.getString("age", "");
        server = prefs.getString("current_server", "");
        Set <String> servers = new HashSet();
        servers = prefs.getStringSet("servers", servers);
        boolean found=false;
        if(isServerAvailable(server))
            sendToServer();
        else
        { 
            for(String ser : servers)
            {
                if(isServerAvailable(ser))
                {
                    found=true;
                    servers.add(server);
                    server=ser;
                    editor.putString("current_server", ser);
                    servers.remove(ser);
                    editor.commit();
                    sendToServer();
                }
            }
            if(!found)
            {
                Toast.makeText(this, "No servers are available at the moment...", Toast.LENGTH_LONG).show();
                this.stopSelf();
            }
                
        }
        
        try {
          writer = openFileOutput("dataSensor.xml",MODE_PRIVATE);
        } catch (FileNotFoundException ex) {
              Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
              Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
          }

        try {
              writer.write((new String()).getBytes());
        } catch (IOException ex) {
             Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
            writer.write(("<logs user=\""+username+"\" age=\""+age+"\">").getBytes());

        } catch (FileNotFoundException ex) {
            Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      return Service.START_NOT_STICKY;
      
  }
  
  public boolean isServerAvailable(String server)
  {
            boolean avail=false;
            HttpClient client;
            HttpGet get = new HttpGet(server+"master");
            HttpParams httpParameters = new BasicHttpParams();
            int timeoutConnection = 2500;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            client = new DefaultHttpClient(httpParameters);
            try {
                HttpResponse res = client.execute(get);
                if(res.getStatusLine().getStatusCode()==400)
                {
                	return avail;
                }
            } catch (IOException ex) {
                Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
                return avail;
            } 
            avail=true;
            return avail;
  }
  
  public String readFromFile()
  {
        String s,finalS="";
        try {
            FileInputStream fIn = openFileInput("dataSensor.xml");
                
            InputStreamReader isr = new InputStreamReader(fIn);

            // Fill the Buffer with data from the file
            BufferedReader br = new BufferedReader(isr);

            while ((s = br.readLine()) != null) {
                finalS+=s;
            }
            isr.close();
            fIn.close();
        } catch (IOException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return finalS;
  }
  
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
            = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

  public void sendToServer()
  {

      try
          
      {

            writer = openFileOutput("dataSensor.xml",MODE_APPEND | MODE_WORLD_READABLE);
            writer.write("</logs>\n".getBytes());

            String file=readFromFile();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String username=prefs.getString("username", "");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(server+"master/add");
            try 
            {
              List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
              nameValuePairs.add(new BasicNameValuePair("id",username));
              nameValuePairs.add(new BasicNameValuePair("data",file));
              post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
              HttpResponse response;
              if(file.contains("timestamp")) // gia na mi stelnei xoris na exei kapoio log.
              {	
                  Log.i("DATA",file);
            	  response = client.execute(post);
              }
            } 
            catch (IOException ex) 
            {   
               Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
      } 
      catch (FileNotFoundException ex) 
      {   
         Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
            Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
      }
      finally
      {
          /*try {
              writer.close();
          } catch (IOException ex) {
              Logger.getLogger(DataSendingService.class.getName()).log(Level.SEVERE, null, ex);
          }*/
      }
  }
  
  @Override
  public IBinder onBind(Intent intent) {
  //TODO for communication return IBinder implementation
    return null;
  }
  
} 