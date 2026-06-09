package com.aaron.cameraparams

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aaron.cameraparams.CameraParamsActivity.MyAdapter.MyViewHolder
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CameraParamsActivity : AppCompatActivity() {
    private var cameraController: CameraParamsHelper? = null
    private var viewAdapter: MyAdapter? = null
    private var viewManager: RecyclerView.LayoutManager? = null
    private var camera_chooser: Spinner? = null
    private var list_content: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera_chooser = findViewById<Spinner>(R.id.camera_chooser)
        list_content = findViewById<RecyclerView>(R.id.list_content)
        cameraController = CameraParamsHelper(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
            return
        }
        bindCameraList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = getMenuInflater()
        inflater.inflate(R.menu.camera_params, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == R.id.menu_export) {
            runExportAllParams()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun runExportAllParams() {
        object : Thread("run Export All Params") {
            override fun run() {
                val `object` = CameraObject()
                val cameras = cameraController!!.getSupportCameraIds()
                val cameraIdList: MutableList<CameraId?> = ArrayList<CameraId?>()
                for (item in cameras) {
                    val cameraId = CameraId()
                    cameraId.cameraId = item
                    val paramsList: MutableList<CameraParamaters?> = ArrayList<CameraParamaters?>()
                    val keys = cameraController!!.getAvailableKeys()
                    for (key in keys) {
                        val params = CameraParamaters()
                        params.key = key.getName()
                        params.value = cameraController!!.getCharacteristicInfo(key)
                        paramsList.add(params)
                    }
                    cameraId.paramaters = paramsList
                    cameraIdList.add(cameraId)
                }
                `object`.cameras = cameraIdList
                val gson = Gson()
                val json = gson.toJson(`object`)
                var writer: FileWriter? = null
                try {
                    writer = FileWriter(File(getExternalCacheDir(), "cameraParams.json"))
                    writer.append(json)
                    writer.flush()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    if (writer != null) {
                        try {
                            writer.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }.start()
    }

    private fun bindCameraList() {
        viewManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        viewAdapter = MyAdapter()
        list_content!!.setHasFixedSize(true)
        list_content!!.setLayoutManager(viewManager)
        list_content!!.setAdapter(viewAdapter)
        val cameras = cameraController!!.getSupportCameraIds()
        camera_chooser!!.setAdapter(
            ArrayAdapter<String?>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                cameras
            )
        )
        camera_chooser!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                cameraController!!.bindCameraId(position)
                viewAdapter!!.setDataset(cameraController!!.getAvailableKeys())
                viewAdapter!!.notifyDataSetChanged()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        })
    }


    private fun requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
//            ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//                ErrorDialog.newInstance(getString(R.string.request_permission))
//                    .show(getChildFragmentManager(), FRAGMENT_DIALOG)
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                bindCameraList()
            }
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyViewHolder?>() {
        private val myDataset: MutableList<CameraCharacteristics.Key<*>?> = ArrayList<Any?>()

        private inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var keyView: TextView
            var keyValue: TextView

            init {
                keyView = itemView.findViewById<TextView>(R.id.tvw_key)
                keyValue = itemView.findViewById<TextView>(R.id.tvw_value)
            }
        }

        fun setDataset(keys: MutableList<CameraCharacteristics.Key<*>>) {
            myDataset.clear()
            myDataset.addAll(keys)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyViewHolder {
            val textView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.characterstics_item, parent, false)
            return MyViewHolder(textView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            /*if (position == 3) {
                Log.d("aaron", "onBindViewHolder " + myDataset[position].name)
            }*/
            holder.keyView.setText(myDataset.get(position)!!.getName())
            val t = cameraController!!.getCharacteristicInfo(myDataset.get(position))

            /*if  (t ) {
                is IntArray -> holder.keyValue.text = Arrays.toString(t)
                is FloatArray -> holder.keyValue.text = Arrays.toString(t)
                is BooleanArray -> holder.keyValue.text = Arrays.toString(t)
                is Array<Range> -> holder.keyView.text = Arrays.toString(t)
                else -> holder.keyValue.text = t.toString()
            }*/
            holder.keyValue.setText(t)
        }

        override fun getItemCount(): Int {
            return myDataset.size
        }
    }


    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1
    }
}
