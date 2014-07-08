package android.mema;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DataSendingService extends Service {

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    //TODO do something useful
      Log.i("LALALA","LOLOLO");
    return Service.START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
  //TODO for communication return IBinder implementation
    return null;
  }
} 