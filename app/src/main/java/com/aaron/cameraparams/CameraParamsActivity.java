package com.aaron.cameraparams;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
            Object t =  cameraController.getCharacteristicInfo(myDataset.get(position));
            /*if  (t ) {
                is IntArray -> holder.keyValue.text = Arrays.toString(t)
                is FloatArray -> holder.keyValue.text = Arrays.toString(t)
                is BooleanArray -> holder.keyValue.text = Arrays.toString(t)
                is Array<Range> -> holder.keyView.text = Arrays.toString(t)
                else -> holder.keyValue.text = t.toString()
            }*/

            holder.keyValue.setText(formatCameraParams(t));

        }

        @Override
        public int getItemCount() {
            return myDataset.size();
        }
    }

    private static String formatCameraParams(Object t) {
        if (t instanceof int[]) {
            return ((Arrays.toString((int[]) t)));
        } else if (t instanceof float[]) {
            return ((Arrays.toString((float[]) t)));
        } else if (t instanceof boolean[]) {
            return ((Arrays.toString((boolean []) t)));
        } else if (t instanceof Object[]) {
            return ((Arrays.toString((Object[]) t)));
        } else if (t instanceof StreamConfigurationMap){
            return CameraParamsHelper.streamConfigurationMapToString((StreamConfigurationMap)t);
        } else {
            return t.toString();
        }
    }



}
