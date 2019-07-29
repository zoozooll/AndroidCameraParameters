package com.aaron.cameraparams;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CameraParamsActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1;


    private CameraParamsHelper cameraController;
    private MyAdapter viewAdapter;
    private RecyclerView.LayoutManager viewManager;
    private Spinner camera_chooser;
    private RecyclerView list_content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera_chooser = findViewById(R.id.camera_chooser);
        list_content = findViewById(R.id.list_content);
        cameraController = new CameraParamsHelper(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        bindCameraList();
        /*TextureView mytexture = findViewById(R.id.mytexture);
        mytexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("MyTexture", "onSurfaceTextureAvailable");
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d("MyTexture", "onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d("MyTexture", "onSurfaceTextureDestroyed");
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.d("MyTexture", "onSurfaceTextureUpdated");
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_params, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_export:
                    runExportAllParams();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        Log.d("MyTexture", "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
//        Log.d("MyTexture", "onStop");
    }

    private void runExportAllParams() {
        new Thread("run Export All Params"){
            @Override
            public void run() {

                CameraObject object = new CameraObject();
                String[] cameras = cameraController.getSupportCameraIds();
                List<CameraId> cameraIdList = new ArrayList<>();
                for (String item : cameras) {
                    CameraId cameraId = new CameraId();
                    cameraId.cameraId = item;
                    List<CameraParamaters> paramsList = new ArrayList<>();
                    List<CameraCharacteristics.Key<?>> keys = cameraController.getAvailableKeys();
                    for (CameraCharacteristics.Key key : keys) {
                        CameraParamaters params = new CameraParamaters();
                        params.key = key.getName();
                        params.value = cameraController.getCharacteristicInfo(key);
                        paramsList.add(params);
                    }
                    cameraId.paramaters = paramsList;
                    cameraIdList.add(cameraId);
                }
                object.cameras = cameraIdList;
                Gson gson = new Gson();
                String json = gson.toJson(object);
                FileWriter writer = null;
                try {
                    writer = new FileWriter(new File(getExternalCacheDir(), "cameraParams.json"));
                    writer.append(json);
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }

    private void bindCameraList() {
        viewManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        viewAdapter = new MyAdapter();
        list_content.setHasFixedSize(true);
        list_content.setLayoutManager(viewManager);
        list_content.setAdapter(viewAdapter);
        String[] cameras = cameraController.getSupportCameraIds();
        camera_chooser.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, android.R.id.text1, cameras));
        camera_chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cameraController.bindCameraId(position);
                viewAdapter.setDataset(cameraController.getAvailableKeys());
                viewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

    }


    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//            ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG)
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG)
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                bindCameraList();
            }
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<CameraCharacteristics.Key<?>> myDataset = new ArrayList();

        private class MyViewHolder extends RecyclerView.ViewHolder {

            TextView keyView;
            TextView keyValue;
            MyViewHolder(View itemView) {
                super(itemView);
                keyView = itemView.findViewById(R.id.tvw_key);
                keyValue = itemView.findViewById(R.id.tvw_value);
            }

        }

        void setDataset(List<CameraCharacteristics.Key<?>> keys) {
            myDataset.clear();
            myDataset.addAll(keys);
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                       int viewType) {
            View textView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.characterstics_item, parent, false);
            return new MyViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            /*if (position == 3) {
                Log.d("aaron", "onBindViewHolder " + myDataset[position].name)
            }*/
            holder.keyView.setText(myDataset.get(position).getName());
            String t =  cameraController.getCharacteristicInfo(myDataset.get(position));
            /*if  (t ) {
                is IntArray -> holder.keyValue.text = Arrays.toString(t)
                is FloatArray -> holder.keyValue.text = Arrays.toString(t)
                is BooleanArray -> holder.keyValue.text = Arrays.toString(t)
                is Array<Range> -> holder.keyView.text = Arrays.toString(t)
                else -> holder.keyValue.text = t.toString()
            }*/

            holder.keyValue.setText(t);

        }

        @Override
        public int getItemCount() {
            return myDataset.size();
        }
    }




}
