/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package android.mema;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import static android.content.Context.MODE_APPEND;
import static android.content.Context.MODE_WORLD_READABLE;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
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


public class DataCollection extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mLight, mTemperature, mProximity, mHumidity;
    public SensorEventListener mSensorListener ; 
    private FileOutputStream fOut = null;
    private PendingIntent pintent;
    private File myFile;
    private long time;
    private String username;
    private String age;
    private String server;
    
    @Override
    public void onCreate(Bundle icicle) {
        try {
            
                super.onCreate(icicle);
                setContentView(R.layout.datacollection);
                
                
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                username=prefs.getString("username", "");
                age=prefs.getString("age", "");
                server=prefs.getString("current_server", "");

                myFile = new File("/data/data/android.mema/files/dataSensor.xml");
                if(myFile.exists())
                    myFile.delete();
                
                fOut = openFileOutput("dataSensor.xml", MODE_APPEND | MODE_WORLD_READABLE);
                this.registerReceiver(this.PowerConnectionReceiver, 
                  new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                collectData();
                Calendar cal = Calendar.getInstance();

                Intent intent = new Intent(this, DataSendingService.class);
                pintent = PendingIntent.getService(this, 1234567, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                
                AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 10*1000, pintent); 

        } catch (FileNotFoundException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }
    
    public void collectData()
    {
        try {
            
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            mAccelerometer=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mLight=mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mTemperature=mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            mProximity=mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mHumidity=mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String username=prefs.getString("username", "");
            String age=prefs.getString("age", "");
            writeToFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(),fOut);
            writeToFile(("<logs user=\""+username+"\" age=\""+age+"\">").getBytes(),fOut);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalStateException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }
    private BroadcastReceiver PowerConnectionReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) { 
            try {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL;

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                String charging;
                if(isCharging)
                    charging="charging";
                else
                    charging="not charging";
                float batteryPct = (level / (float)scale)*100;

                

                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Battery\">\n<value type=\"level\">"+Math.round(batteryPct)+"</value>\n<value type=\"charging\">"+charging+"</value>\n</log>";
                writeToFile(log.getBytes(),fOut);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };

    
    public void writeToFile(byte[] data, FileOutputStream f) {

        try {
            f.write("\n".getBytes());            
            f.write(data);
        }
        catch (IOException e) {
            Log.e("AndroidSensorList::","File write failed: " + e.toString());
        } 


    }
    
    public String readFromFile()
    {
        String s,finalS="";
        try {
            FileInputStream fIn = openFileInput("dataSensor.xml");
                
            InputStreamReader isr = new InputStreamReader(fIn);

            BufferedReader br = new BufferedReader(isr);

            while ((s = br.readLine()) != null) {
                finalS+=s;
            }
            isr.close();

        } catch (IOException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
            return finalS;
    }
    
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mLight,  SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mTemperature,  SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mProximity,  SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mHumidity,  SensorManager.SENSOR_DELAY_NORMAL);

        }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);

        super.onPause();

    }
    public void onStop(View view) {
        mSensorManager.unregisterListener(this);
        mSensorListener = null;
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        	while (myFile.length()>=10000000)
        	{
        		File tempFile = new File("/data/data/android.mema/files/myTempFile.xml");
        		BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(myFile));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new FileWriter(tempFile));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

        		String currentLine;
        		boolean insideFirstLog=false;
        		boolean endProcess=false;
        		try {
					while((currentLine = reader.readLine()) != null) {
					    String trimmedLine = currentLine.trim();
					    if(insideFirstLog && !trimmedLine.equals("</log>"))
					    {
					    	continue;
					    }
					    else if(insideFirstLog && trimmedLine.equals("</log>"))
					    {
					    	insideFirstLog=false;
					    	endProcess=true;
					    	continue;
					    }
					    if(!insideFirstLog && !endProcess && trimmedLine.contains("timestamp"))
					    {
					    	insideFirstLog=true;
					    	continue;
					    }
					    if(!currentLine.isEmpty())
					    writer.write(currentLine+"\n");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

        		boolean successful = tempFile.renameTo(myFile);
        		try {
					reader.close();
	        		writer.close();
	                fOut = openFileOutput("dataSensor.xml", MODE_APPEND | MODE_WORLD_READABLE);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        	}
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
            {
                long curTime=System.currentTimeMillis();
                if(curTime-time>2000)
                {
                    Log.i("TIMES ARE: ",curTime+"  "+time);
                    try {
                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Accelerometer\">\n<value type=\"xaxis\">"+Float.toString(x)+"</value>\n<value type=\"yaxis\">"+Float.toString(y)+"</value>\n<value type=\"zaxis\">"+Float.toString(z)+"</value>\n</log>";
                        writeToFile(log.getBytes(),fOut);
                        time=curTime;
                    } catch (IllegalArgumentException ex) {
                        Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalStateException ex) {
                        Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            else if (sensor.getType() == Sensor.TYPE_LIGHT) {
            try {
                float lux = event.values[0];

                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Light\">\n<value type=\"lux\">"+lux+"</value>\n</log>";
                writeToFile(log.getBytes(),fOut);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
            else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            try {
                float temp = event.values[0];

                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Temperature\">\n<value type=\"temp\">"+temp+"</value>\n</log>";
                writeToFile(log.getBytes(),fOut);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
            else if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
            try {
                float prox = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Proximity\">\n<value type=\"prox\">"+prox+"</value>\n</log>";
                writeToFile(log.getBytes(),fOut);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
            else if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            try {
                float humidity = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Humidity\">\n<value type=\"humidity\">"+humidity+"</value>\n</log>";
                writeToFile(log.getBytes(),fOut);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalStateException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
        
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
        
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
    public void stopCollection(View view)
    {
        try {
            TextView tv = (TextView) this.findViewById(R.id.stopCollection);
            tv.setVisibility(View.INVISIBLE);
            showToast("Stopping the collection...");
            mSensorManager.unregisterListener(this);
            mSensorListener = null;
            this.unregisterReceiver(this.PowerConnectionReceiver);
            writeToFile("</logs>\n".getBytes(),fOut);
            try {
                   fOut.close();
            } 
            catch (IOException ex) {
                   Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            Intent intent = new Intent(this, DataSendingService.class);
            PendingIntent pintent = PendingIntent.getService(this, 1234567, intent, 0);
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            alarm.cancel(pintent);
            Runnable runnable = new Runnable() {
            @Override
            public void run() {
               SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DataCollection.this);
               final SharedPreferences.Editor editor = prefs.edit();

               Set <String> servers = new HashSet();
               servers = prefs.getStringSet("servers", servers);

               boolean found=false;
               if(!isServerAvailable(server))
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
                        }
                    }
                    if(!found)
                    {
                        // Nothing done here yet...
                        showToast("No servers are available at the moment...");
                        finish();
                    }
                
                }
               String file=readFromFile();
               HttpClient client = new DefaultHttpClient();
               HttpPost post = new HttpPost(server+"master/add");
               
               try
               {
                   List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
                   nameValuePairs.add(new BasicNameValuePair("id",username));
                   nameValuePairs.add(new BasicNameValuePair("data",file));
                   post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                   HttpResponse response;
                   response = client.execute(post);
                   String result = EntityUtils.toString(response.getEntity());
               }
               catch (IOException ex) 
               {   
                   Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
               }
               finish();
           } 
            
       };
       new Thread(runnable).start();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalStateException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void showToast(final String toast)
    {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(DataCollection.this, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
     public void onBackPressed() {
        Toast.makeText(this, "To exit the app you must first stop the data collection. To continue while app is on the background, just press the home button.", Toast.LENGTH_LONG).show();
    }
    
}
