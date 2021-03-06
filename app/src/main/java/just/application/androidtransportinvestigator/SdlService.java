package just.application.androidtransportinvestigator;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;

import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.proxy.RPCRequest;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.smartdevicelink.proxy.callbacks.OnServiceEnded;
import com.smartdevicelink.proxy.callbacks.OnServiceNACKed;
import com.smartdevicelink.proxy.interfaces.IProxyListenerALM;
import com.smartdevicelink.proxy.rpc.AddCommand;
import com.smartdevicelink.proxy.rpc.AddCommandResponse;
import com.smartdevicelink.proxy.rpc.AddSubMenuResponse;
import com.smartdevicelink.proxy.rpc.AlertManeuverResponse;
import com.smartdevicelink.proxy.rpc.AlertResponse;
import com.smartdevicelink.proxy.rpc.ButtonPressResponse;
import com.smartdevicelink.proxy.rpc.ChangeRegistrationResponse;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteCommandResponse;
import com.smartdevicelink.proxy.rpc.DeleteFileResponse;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.smartdevicelink.proxy.rpc.DeleteSubMenuResponse;
import com.smartdevicelink.proxy.rpc.DiagnosticMessageResponse;
import com.smartdevicelink.proxy.rpc.DialNumberResponse;
import com.smartdevicelink.proxy.rpc.EndAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.GenericResponse;
import com.smartdevicelink.proxy.rpc.GetDTCsResponse;
import com.smartdevicelink.proxy.rpc.GetInteriorVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.GetSystemCapabilityResponse;
import com.smartdevicelink.proxy.rpc.GetVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.GetWayPointsResponse;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.ListFiles;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.MenuParams;
import com.smartdevicelink.proxy.rpc.OnAudioPassThru;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnDriverDistraction;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnHashChange;
import com.smartdevicelink.proxy.rpc.OnInteriorVehicleData;
import com.smartdevicelink.proxy.rpc.OnKeyboardInput;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnLockScreenStatus;
import com.smartdevicelink.proxy.rpc.OnPermissionsChange;
import com.smartdevicelink.proxy.rpc.OnRCStatus;
import com.smartdevicelink.proxy.rpc.OnStreamRPC;
import com.smartdevicelink.proxy.rpc.OnSystemRequest;
import com.smartdevicelink.proxy.rpc.OnTBTClientState;
import com.smartdevicelink.proxy.rpc.OnTouchEvent;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.OnWayPointChange;
import com.smartdevicelink.proxy.rpc.PerformAudioPassThruResponse;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.PutFileResponse;
import com.smartdevicelink.proxy.rpc.ReadDIDResponse;
import com.smartdevicelink.proxy.rpc.ResetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.ScrollableMessageResponse;
import com.smartdevicelink.proxy.rpc.SendHapticDataResponse;
import com.smartdevicelink.proxy.rpc.SendLocationResponse;
import com.smartdevicelink.proxy.rpc.SetAppIconResponse;
import com.smartdevicelink.proxy.rpc.SetDisplayLayoutResponse;
import com.smartdevicelink.proxy.rpc.SetGlobalPropertiesResponse;
import com.smartdevicelink.proxy.rpc.SetInteriorVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SetMediaClockTimerResponse;
import com.smartdevicelink.proxy.rpc.ShowConstantTbtResponse;
import com.smartdevicelink.proxy.rpc.ShowResponse;
import com.smartdevicelink.proxy.rpc.SliderResponse;
import com.smartdevicelink.proxy.rpc.SpeakResponse;
import com.smartdevicelink.proxy.rpc.StreamRPCResponse;
import com.smartdevicelink.proxy.rpc.SubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.SubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.SubscribeWayPointsResponse;
import com.smartdevicelink.proxy.rpc.SystemRequestResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeButtonResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeVehicleDataResponse;
import com.smartdevicelink.proxy.rpc.UnsubscribeWayPointsResponse;
import com.smartdevicelink.proxy.rpc.UpdateTurnListResponse;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.SdlDisconnectedReason;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.transport.BTTransportConfig;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;
import com.smartdevicelink.transport.TransportConstants;
import com.smartdevicelink.transport.USBTransportConfig;
import com.smartdevicelink.util.CorrelationIdGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A SdlService manages the lifecycle of an SDL Proxy.
 * The SDLService enables auto-start by creating the SDL Proxy, which then waits for a connection from SDL.
 * This file also sends and receives messages to and from SDL after connected.
 **/
public class SdlService extends Service implements IProxyListenerALM {

    private static final String TAG = "SdlService";

    private static final String APP_NAME = "SDL-Android Transport Investigator";
    private static final String APP_ID = "8675309";

    private Defines.TransportType transportType = Defines.TransportType.NONE;

    private static final String IMAGE_FILENAME = "jarvis_icon.png";
    private static final String ICON_FILENAME = "hello_sdl_icon.png";

    private static final String WELCOME_SHOW = "Welcome to Transport Investigator";
    private static final String WELCOME_JARVIS_SPEAK = "Mr. Stark, we need to talk";

    private static final String TEST_COMMAND_NAME = "Test Command";
    private static final int TEST_COMMAND_ID = 1;

    private static final int FOREGROUND_SERVICE_ID = 111;

    // SDL's default port is 12345
    private static final int TCP_PORT = 12345;

    // variable to create and call functions of the SyncProxy
    private SdlProxyALM proxy = null;

    private int iconCorrelationId;
    private List<String> remoteFiles;

    private boolean firstNonHmiNone = true;
    @SuppressWarnings("unused")
    private boolean isVehicleDataSubscribed = false;

    BroadcastLogger broadcastLogger;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        remoteFiles = new ArrayList<>();
        broadcastLogger = new BroadcastLogger(this, Defines.BroadcastLoggerId.MAIN_ACTIVITY_LOGGER.toString());

        //Because of Android Oreo's requirements, it is mandatory that services enter the foreground for long running tasks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterForeground();
        }
    }

    @SuppressLint("NewApi")
    public void enterForeground() {
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Connected through SDL")
                .setSmallIcon(R.drawable.ic_sdl)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .build();
        startForeground(FOREGROUND_SERVICE_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        transportType = (Defines.TransportType)intent.getSerializableExtra( "TransportType");
        int bluetoothSecurityLevel = intent.getIntExtra("BluetoothSecurityLevel", 0);
        String userIp = intent.getStringExtra((String) "UserIp");

        startProxy(intent, userIp, bluetoothSecurityLevel);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        disposeSyncProxy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        super.onDestroy();
    }

    private void startProxy(Intent intent,
                            String userIp,
                            int bluetoothSecurityLevel) {

        broadcastLogger.Info(TAG, "Trying to start proxy. TransportType is " + transportType);

        boolean forceConnect = intent != null && intent.getBooleanExtra(TransportConstants.FORCE_TRANSPORT_CONNECTED, false);

        if (null == proxy) {
            try {
                broadcastLogger.Info(TAG, "Starting SDL Proxy");
                BaseTransportConfig transport = null;

                switch (transportType) {
                    case USB:
                        if (intent != null && intent.hasExtra(UsbManager.EXTRA_ACCESSORY)) { //If we want to support USB transport
                            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.HONEYCOMB) {
                                broadcastLogger.Error(TAG, "Unable to start proxy. Android OS version is too low");
                                return;
                            } else {
                                transport = new USBTransportConfig(getBaseContext(), (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY));
                                broadcastLogger.Debug(TAG, "USB created.");
                            }
                        } else {
                            broadcastLogger.Error(TAG, "Unable to start proxy. " + (intent != null ? "USB doesn't have EXTRA_ACCESSORY." : "Intent is null."));
                        }
                        break;

                    case TCP:
                        transport = new TCPTransportConfig(TCP_PORT, userIp, true);
                        break;

                    case MBT:
                        transport = new MultiplexTransportConfig(this, APP_ID, bluetoothSecurityLevel);
                        break;

                    case LBT:
                        transport = new BTTransportConfig();
                        break;

                    default:
                        break;
                }

                if (transport != null) {
                    proxy = new SdlProxyALM(this, APP_NAME, true, APP_ID, transport);
                } else {
                    broadcastLogger.Error(TAG, "Proxy was not created. Input params: transportType = " + transportType +
                            "; bluetoothSecurityLevel = " + bluetoothSecurityLevel);
                }

            } catch (SdlException e) {
                e.printStackTrace();
                // error creating proxy, returned proxy = null
                if (null == proxy) {
                    stopSelf();
                }
            }
        } else if(forceConnect) {
            proxy.forceOnConnected();
        }
    }

    private void disposeSyncProxy() {

        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                proxy = null;
            }
        }

        this.firstNonHmiNone = true;
        this.isVehicleDataSubscribed = false;
    }

    /**
     * Will show a sample test message on screen as well as speak a sample test message
     */
    private void showTest(){
        try {
            proxy.show(TEST_COMMAND_NAME, "Command has been selected", TextAlignment.CENTERED, CorrelationIdGenerator.generateId());
            proxy.speak(TEST_COMMAND_NAME, CorrelationIdGenerator.generateId());
        } catch (SdlException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Add commands for the app on SDL.
     */
    private void sendCommands() {
        AddCommand command = new AddCommand();
        MenuParams params = new MenuParams();
        params.setMenuName(TEST_COMMAND_NAME);
        command.setCmdID(TEST_COMMAND_ID);
        command.setMenuParams(params);
        command.setVrCommands(Collections.singletonList(TEST_COMMAND_NAME));
        sendRpcRequest(command);
    }
    /**
     * Sends an RPC Request to the connected head unit. Automatically adds a correlation id.
     * @param request the rpc request that is to be sent to the module
     */
    private void sendRpcRequest(RPCRequest request){
        try {
            proxy.sendRPCRequest(request);
        } catch (SdlException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends the app icon through the uploadImage method with correct params
     */
    private void sendIcon(){
        iconCorrelationId = CorrelationIdGenerator.generateId();
        uploadImage(R.mipmap.ic_launcher, ICON_FILENAME, iconCorrelationId, true);
    }

    /**
     * This method will help upload an image to the head unit
     * @param resource the R.drawable.__ value of the image you wish to send
     * @param imageName the filename that will be used to reference this image
     * @param correlationId the correlation id to be used with this request. Helpful for monitoring putfileresponses
     * @param isPersistent tell the system if the file should stay or be cleared out after connection.
     */
     @SuppressWarnings("SameParameterValue")
     private void uploadImage(int resource, String imageName, int correlationId, boolean isPersistent){
        PutFile putFile = new PutFile();
        putFile.setFileType(FileType.GRAPHIC_PNG);
        putFile.setSdlFileName(imageName);
        putFile.setCorrelationID(correlationId);
        putFile.setPersistentFile(isPersistent);
        putFile.setSystemFile(false);
        putFile.setBulkData(contentsOfResource(resource));

        try {
            proxy.sendRPCRequest(putFile);
        } catch (SdlException e) {
            e.printStackTrace();
        }
     }

    /**
     * Helper method to take resource files and turn them into byte arrays
     * @param resource Resource file id.
     * @return Resulting byte array.
     */
    private byte[] contentsOfResource(int resource) {
        InputStream is = null;
        try {
            is = getResources().openRawResource(resource);
            ByteArrayOutputStream os = new ByteArrayOutputStream(is.available());
            final int bufferSize = 4096;
            final byte[] buffer = new byte[bufferSize];
            int available;
            while ((available = is.read(buffer)) >= 0) {
                os.write(buffer, 0, available);
            }
            return os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            broadcastLogger.Warning(TAG, "Can't read icon file");
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onProxyClosed(String info, Exception e, SdlDisconnectedReason reason) {
        stopSelf();
        
        if(reason.equals(SdlDisconnectedReason.LANGUAGE_CHANGE) && transportType.equals(Defines.TransportType.MBT)){
            Intent intent = new Intent(TransportConstants.START_ROUTER_SERVICE_ACTION);
            intent.putExtra(SdlReceiver.RECONNECT_LANG_CHANGE, true);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {

        broadcastLogger.Info(TAG, "HMI state " + notification.getHmiLevel().name());

        switch (notification.getHmiLevel()) {
            case HMI_FULL:
                if (notification.getFirstRun()) {
                    performWelcomeMessage();
                }

                if (firstNonHmiNone) {
                    sendCommands();
                    firstNonHmiNone = false;
                }
                break;

            case HMI_LIMITED:
            case HMI_BACKGROUND:
                if (firstNonHmiNone) {
                    sendCommands();
                    firstNonHmiNone = false;
                }

                break;

            case HMI_NONE:
                if(notification.getFirstRun()) {
                    uploadImages();
                }
                break;

            default:
                return;
        }
    }

    /**
     * Will show a sample welcome message on screen as well as speak a sample welcome message
     */
    private void performWelcomeMessage(){
        try {
            Image image = new Image();
            image.setValue(IMAGE_FILENAME);
            image.setImageType(ImageType.DYNAMIC);

            //Set the welcome message on screen
            proxy.show(APP_NAME, WELCOME_SHOW, null, null, null, null, null, image, null, null, TextAlignment.CENTERED, CorrelationIdGenerator.generateId());

            //Say the welcome message
            proxy.speak(WELCOME_JARVIS_SPEAK, CorrelationIdGenerator.generateId());

        } catch (SdlException e) {
            e.printStackTrace();
        }

    }

    /**
     *  Requests list of images to SDL, and uploads images that are missing.
     */
    private void uploadImages(){
        ListFiles listFiles = new ListFiles();
        listFiles.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if(response.getSuccess()){
                    remoteFiles = ((ListFilesResponse) response).getFilenames();
                }

                // Check the mutable set for the AppIcon
                // If not present, upload the image
                if(remoteFiles== null || !remoteFiles.contains(SdlService.ICON_FILENAME)){
                    sendIcon();
                }else{
                    // If the file is already present, send the SetAppIcon request
                    try {
                        proxy.setappicon(ICON_FILENAME, CorrelationIdGenerator.generateId());
                    } catch (SdlException e) {
                        e.printStackTrace();
                    }
                }

                // Check the mutable set for the SDL image
                // If not present, upload the image
                if(remoteFiles== null || !remoteFiles.contains(SdlService.IMAGE_FILENAME)){
                    uploadImage(R.drawable.jarvis_icon, IMAGE_FILENAME, CorrelationIdGenerator.generateId(), true);
                }
            }
        });
        this.sendRpcRequest(listFiles);
    }

    @Override
    public void onListFilesResponse(ListFilesResponse response) {
        broadcastLogger.Info(TAG, "onListFilesResponse from SDL ");
    }

    @Override
    public void onPutFileResponse(PutFileResponse response) {
        broadcastLogger.Info(TAG, "onPutFileResponse from SDL");
        if(response.getCorrelationID() == iconCorrelationId){ //If we have successfully uploaded our icon, we want to set it
            try {
                proxy.setappicon(ICON_FILENAME, CorrelationIdGenerator.generateId());
            } catch (SdlException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOnLockScreenNotification(OnLockScreenStatus notification) {
        broadcastLogger.Info(TAG, "OnLockScreen notification from SDL: " + notification);
    }

    @Override
    public void onOnCommand(OnCommand notification){
        Integer id = notification.getCmdID();
        if(id != null){
            switch(id){
                case TEST_COMMAND_ID:
                    showTest();
                    break;
            }
        }
    }

    /**
     *  Callback method that runs when the add command response is received from SDL.
     */
    @Override
    public void onAddCommandResponse(AddCommandResponse response) {
        broadcastLogger.Info(TAG, "AddCommand response from SDL: " + response.getResultCode().name());

    }

    /**
    * Vehicle Data
    */
    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {
        broadcastLogger.Info(TAG, "Permision changed: " + notification);
    }

    /**
     * Rest of the SDL callbacks from the head unit
     */

    @Override
    public void onSubscribeVehicleDataResponse(SubscribeVehicleDataResponse response) {
        broadcastLogger.Info(TAG, "Subscribe Vehicle Data Response notification from SDL");
        if(response.getSuccess()){
            broadcastLogger.Info(TAG, "Subscribed to vehicle data");
            this.isVehicleDataSubscribed = true;

        }
    }

    @Override
    public void onOnVehicleData(OnVehicleData notification) {
        broadcastLogger.Info(TAG, "Vehicle data notification from SDL");
    }

    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {
        broadcastLogger.Info(TAG, "AddSubMenu response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
        broadcastLogger.Info(TAG, "CreateInteractionChoiceSet response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onAlertResponse(AlertResponse response) {
        broadcastLogger.Info(TAG, "Alert response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {
        broadcastLogger.Info(TAG, "DeleteCommand response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
        broadcastLogger.Info(TAG, "DeleteInteractionChoiceSet response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
        broadcastLogger.Info(TAG, "DeleteSubMenu response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {
        broadcastLogger.Info(TAG, "PerformInteraction response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onResetGlobalPropertiesResponse(
            ResetGlobalPropertiesResponse response) {
        broadcastLogger.Info(TAG, "ResetGlobalProperties response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
        broadcastLogger.Info(TAG, "SetGlobalProperties response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
        broadcastLogger.Info(TAG, "SetMediaClockTimer response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onShowResponse(ShowResponse response) {
        broadcastLogger.Info(TAG, "Show response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {
        broadcastLogger.Info(TAG, "SpeakCommand response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        broadcastLogger.Info(TAG, "OnButtonEvent notification from SDL: " + notification);
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        broadcastLogger.Info(TAG, "OnButtonPress notification from SDL: " + notification);
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
        broadcastLogger.Info(TAG, "SubscribeButton response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
        broadcastLogger.Info(TAG, "UnsubscribeButton response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {
        broadcastLogger.Info(TAG, "OnTBTClientState notification from SDL: " + notification);
    }

    @Override
    public void onUnsubscribeVehicleDataResponse(
            UnsubscribeVehicleDataResponse response) {
        broadcastLogger.Info(TAG, "UnsubscribeVehicleData response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onGetVehicleDataResponse(GetVehicleDataResponse response) {
        broadcastLogger.Info(TAG, "GetVehicleData response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onReadDIDResponse(ReadDIDResponse response) {
        broadcastLogger.Info(TAG, "ReadDID response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onGetDTCsResponse(GetDTCsResponse response) {
        broadcastLogger.Info(TAG, "GetDTCs response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onPerformAudioPassThruResponse(PerformAudioPassThruResponse response) {
        broadcastLogger.Info(TAG, "PerformAudioPassThru response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onEndAudioPassThruResponse(EndAudioPassThruResponse response) {
        broadcastLogger.Info(TAG, "EndAudioPassThru response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnAudioPassThru(OnAudioPassThru notification) {
        broadcastLogger.Info(TAG, "OnAudioPassThru notification from SDL: " + notification );
    }

    @Override
    public void onDeleteFileResponse(DeleteFileResponse response) {
        broadcastLogger.Info(TAG, "DeleteFile response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSetAppIconResponse(SetAppIconResponse response) {
        broadcastLogger.Info(TAG, "SetAppIcon response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onScrollableMessageResponse(ScrollableMessageResponse response) {
        broadcastLogger.Info(TAG, "ScrollableMessage response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onChangeRegistrationResponse(ChangeRegistrationResponse response) {
        broadcastLogger.Info(TAG, "ChangeRegistration response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSetDisplayLayoutResponse(SetDisplayLayoutResponse response) {
        broadcastLogger.Info(TAG, "SetDisplayLayout response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnLanguageChange(OnLanguageChange notification) {
        broadcastLogger.Info(TAG, "OnLanguageChange notification from SDL: " + notification);
    }

    @Override
    public void onSliderResponse(SliderResponse response) {
        broadcastLogger.Info(TAG, "Slider response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnHashChange(OnHashChange notification) {
        broadcastLogger.Info(TAG, "OnHashChange notification from SDL: " + notification);
    }

    @Override
    public void onOnSystemRequest(OnSystemRequest notification) {
        broadcastLogger.Info(TAG, "OnSystemRequest notification from SDL: " + notification);
    }

    @Override
    public void onSystemRequestResponse(SystemRequestResponse response) {
        broadcastLogger.Info(TAG, "SystemRequest response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnKeyboardInput(OnKeyboardInput notification) {
        broadcastLogger.Info(TAG, "OnKeyboardInput notification from SDL: " + notification);
    }

    @Override
    public void onOnTouchEvent(OnTouchEvent notification) {
        broadcastLogger.Info(TAG, "OnTouchEvent notification from SDL: " + notification);
    }

    @Override
    public void onDiagnosticMessageResponse(DiagnosticMessageResponse response) {
        broadcastLogger.Info(TAG, "DiagnosticMessage response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnStreamRPC(OnStreamRPC notification) {
        broadcastLogger.Info(TAG, "OnStreamRPC notification from SDL: " + notification);
    }

    @Override
    public void onStreamRPCResponse(StreamRPCResponse response) {
        broadcastLogger.Info(TAG, "StreamRPC response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onDialNumberResponse(DialNumberResponse response) {
        broadcastLogger.Info(TAG, "DialNumber response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSendLocationResponse(SendLocationResponse response) {
        broadcastLogger.Info(TAG, "SendLocation response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onServiceEnded(OnServiceEnded serviceEnded) {

    }

    @Override
    public void onServiceNACKed(OnServiceNACKed serviceNACKed) {

    }

    @Override
    public void onShowConstantTbtResponse(ShowConstantTbtResponse response) {
        broadcastLogger.Info(TAG, "ShowConstantTbt response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onAlertManeuverResponse(AlertManeuverResponse response) {
        broadcastLogger.Info(TAG, "AlertManeuver response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onUpdateTurnListResponse(UpdateTurnListResponse response) {
        broadcastLogger.Info(TAG, "UpdateTurnList response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onServiceDataACK(int dataSize) {

    }

    @Override
    public void onGetWayPointsResponse(GetWayPointsResponse response) {
        broadcastLogger.Info(TAG, "GetWayPoints response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSubscribeWayPointsResponse(SubscribeWayPointsResponse response) {
        broadcastLogger.Info(TAG, "SubscribeWayPoints response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onUnsubscribeWayPointsResponse(UnsubscribeWayPointsResponse response) {
        broadcastLogger.Info(TAG, "UnsubscribeWayPoints response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnWayPointChange(OnWayPointChange notification) {
        broadcastLogger.Info(TAG, "OnWayPointChange notification from SDL: " + notification);
    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
    }

    @Override
    public void onError(String info, Exception e) {
    }

    @Override
    public void onGenericResponse(GenericResponse response) {
        broadcastLogger.Info(TAG, "Generic response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onGetSystemCapabilityResponse(GetSystemCapabilityResponse response) {
        broadcastLogger.Info(TAG, "GetSystemCapabilityResponse from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSendHapticDataResponse(SendHapticDataResponse response){
        broadcastLogger.Info(TAG, "SendHapticData response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onButtonPressResponse(ButtonPressResponse response) {
        broadcastLogger.Info(TAG, "ButtonPress response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onSetInteriorVehicleDataResponse(SetInteriorVehicleDataResponse response) {
        broadcastLogger.Info(TAG, "SetInteriorVehicleData response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onGetInteriorVehicleDataResponse(GetInteriorVehicleDataResponse response) {
        broadcastLogger.Info(TAG, "GetInteriorVehicleData response from SDL: " + response.getResultCode().name() + " Info: " + response.getInfo());
    }

    @Override
    public void onOnInteriorVehicleData(OnInteriorVehicleData notification) {
        broadcastLogger.Info(TAG, "OnInteriorVehicleData from SDL: " + notification);
    }

    @Override
    public void onOnRCStatus(OnRCStatus status) {
        broadcastLogger.Info(TAG, "onOnRCStatus from SDL: " + status);
    }
}
