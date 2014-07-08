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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author lefteris
 */
public class DataCollection extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mLight, mTemperature, mProximity, mHumidity;
    public SensorEventListener mSensorListener ; 
    private FileOutputStream fOut = null;
    private long time;
    String username;
    String age;

    @Override
    public void onCreate(Bundle icicle) {
        try {
                super.onCreate(icicle);
                setContentView(R.layout.datacollection);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                username=prefs.getString("username", "");
                age=prefs.getString("age", "");
                Toast.makeText(this, "Collecting data", 10000).show();

                File myFile = new File("/data/data/android.mema/files/dataSensor.xml");
                if(myFile.exists())
                    myFile.delete();

                fOut = openFileOutput("dataSensor.xml", MODE_APPEND | MODE_WORLD_READABLE);
                this.registerReceiver(this.PowerConnectionReceiver, 
                  new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                collectData();
                Calendar cal = Calendar.getInstance();

                Intent intent = new Intent(this, DataSendingService.class);
                PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);

                AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                // Start every 30 seconds
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
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        mAccelerometer=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLight=mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mTemperature=mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mProximity=mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mHumidity=mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username=prefs.getString("username", "");
        String age=prefs.getString("age", "");
        writeToFile("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
        writeToFile(("<logs user=\""+username+"\" age=\""+age+"\">").getBytes());

        /*List<String> listSensorType = new ArrayList<String>();
        for(int i=0; i<deviceSensors.size(); i++){
            System.out.println("Inside list sensors:::::::");
            listSensorType.add((i+1)+" "+deviceSensors.get(i).getName());
            //mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(listSensor.get(i).getType()), SensorManager.SENSOR_DELAY_NORMAL);
            writeToFile(deviceSensors.get(i).getName().getBytes() );

        }*/
        

    }
    private BroadcastReceiver PowerConnectionReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) { 
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
            writeToFile(log.getBytes());
        }
    };

    
    public void writeToFile(byte[] data) {

        try {
            fOut.write("\n".getBytes());            
            fOut.write(data);
            //Log.i("File writing stuff", data.toString());
            //fOut.close();

        }
        catch (IOException e) {
            Log.e("AndroidSensorList::","File write failed: " + e.toString());
        } 

    }
    
    /*public void readFromFile()
    {
        try {
            FileInputStream fIn = openFileInput("dataSensor.xml");
                
            InputStreamReader isr = new InputStreamReader(fIn);

            // Fill the Buffer with data from the file
            BufferedReader br = new BufferedReader(isr);

            String s;
            while ((s = br.readLine()) != null) {
                //Log.i("File Reading stuff", s);
            }
            isr.close();


        } catch (IOException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }

    }*/
    
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
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
            {
                long curTime=System.currentTimeMillis();
                if(curTime-time>1000)
                {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];
                    
                    String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Accelerometer\">\n<value type=\"xaxis\">"+Float.toString(x)+"</value>\n<value type=\"yaxis\">"+Float.toString(y)+"</value>\n<value type=\"zaxis\">"+Float.toString(z)+"</value>\n</log>";
                    writeToFile(log.getBytes());
                    time=curTime;
                }
            }
            else if (sensor.getType() == Sensor.TYPE_LIGHT) {
                float lux = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Light\">\n<value type=\"lux\">"+lux+"</value>\n</log>";
                writeToFile(log.getBytes());
            }
            else if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                float temp = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Temperature\">\n<value type=\"temp\">"+temp+"</value>\n</log>";
                writeToFile(log.getBytes());
            }
            else if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float prox = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Proximity\">\n<value type=\"prox\">"+prox+"</value>\n</log>";
                writeToFile(log.getBytes());
            }
            else if (sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                float humidity = event.values[0];
                String log="<log timestamp=\""+System.currentTimeMillis()+"\" type=\"Humidity\">\n<value type=\"humidity\">"+humidity+"</value>\n</log>";
                writeToFile(log.getBytes());
            }

        
    }

    public void onAccuracyChanged(Sensor sensor, int i) {
        
    }

    public void stopCollection(View view)
    {
        try {
            String file = "";
            mSensorManager.unregisterListener(this);
            mSensorListener = null;
            this.unregisterReceiver(this.PowerConnectionReceiver);
            writeToFile("</logs>\n".getBytes());
            //readFromFile();
            fOut.close();
            
            FileInputStream fIn = openFileInput("dataSensor.xml");;
            InputStreamReader isr = new InputStreamReader(fIn);

            BufferedReader br = new BufferedReader(isr);

            String s;
            try {
                while ((s = br.readLine()) != null) {
                    file+=s;
                }
            } catch (IOException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                isr.close();
            } catch (IOException ex) {
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://donald.di.uoa.gr:8580/mde519Server/master/add");
            try
            {
                List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("id",username));
                nameValuePairs.add(new BasicNameValuePair("data",file));
                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response;
                response = client.execute(post);
                String result = EntityUtils.toString(response.getEntity());
                Log.i("VVVVV", result);
            }
            catch (IOException ex) 
            {   
                Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
            }
            stopService(new Intent(this, DataSendingService.class));
            finish();
        } catch (IOException ex) {
            Logger.getLogger(DataCollection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
     public void onBackPressed() {
        Toast.makeText(this, "To exit the app you must first stop the data collection. To continue while app is on the background, just press the home button.", 10000).show();
    }
    
}
