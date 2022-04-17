package fuck.location.app.ui.activities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import com.idanatz.oneadapter.OneAdapter
import com.idanatz.oneadapter.external.event_hooks.ClickEventHook
import com.idanatz.oneadapter.external.event_hooks.SwipeEventHook
import com.idanatz.oneadapter.external.modules.EmptinessModule
import com.idanatz.oneadapter.external.modules.ItemModule
import com.scwang.smart.refresh.layout.api.RefreshLayout
import fuck.location.R
import fuck.location.app.ui.models.FakeLocation
import fuck.location.app.ui.models.FakeLocationListModel
import fuck.location.databinding.ActivityFakelocationListBinding
import fuck.location.xposed.helpers.ConfigGateway
import kotlin.concurrent.thread


@ExperimentalStdlibApi
class FakeLocationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: RefreshLayout

    private lateinit var binding: ActivityFakelocationListBinding
    private lateinit var oneAdapter: OneAdapter
    private var fakeLocationList: MutableList<FakeLocationListModel> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ConfigGateway.get().setCustomContext(applicationContext)

        binding = ActivityFakelocationListBinding.inflate(layoutInflater)
        recyclerView = binding.recycler
        oneAdapter = OneAdapter(recyclerView) {
            itemModules += FakeLocationListModule(this@FakeLocationsActivity)
            emptinessModule = EmptyListModule()
        }

        recyclerView = binding.recycler
        recyclerView.layoutManager = LinearLayoutManager(this)

        refreshLayout = binding.refreshLayout
        refreshLayout.setOnRefreshListener { refresh() }.autoRefresh()

        setContentView(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fakelocation_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> {
                //add fakelocation setting
                setFakeLocation()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    @ExperimentalStdlibApi
    private fun setFakeLocation() {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.custom_location_dialog)
            customView(
                R.layout.custom_view_fakelocation,
                scrollable = true,
                horizontalPadding = true
            )

            ConfigGateway.get().setCustomContext(applicationContext)

            positiveButton(R.string.custom_location_dialog_save) { dialog ->
                val yInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_y)
                val xInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_x)
                val eciInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_eci)
                val pciInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_pci)
                val tacInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_tac)
                val earfcnInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_earfcn)
                val bandwidthInput: EditText = dialog.getCustomView()
                    .findViewById(R.id.custom_view_fl_bandwidth)

                xInput.setText(xInput.text.takeIf { xInput.text.isNotEmpty() } ?: "0.0")
                yInput.setText(yInput.text.takeIf { yInput.text.isNotEmpty() } ?: "0.0")
                eciInput.setText(eciInput.text.takeIf { eciInput.text.isNotEmpty() } ?: "0")
                pciInput.setText(pciInput.text.takeIf { pciInput.text.isNotEmpty() } ?: "0")
                tacInput.setText(tacInput.text.takeIf { tacInput.text.isNotEmpty() } ?: "0")
                earfcnInput.setText(earfcnInput.text.takeIf { earfcnInput.text.isNotEmpty() }
                    ?: "0")
                bandwidthInput.setText(bandwidthInput.text.takeIf { bandwidthInput.text.isNotEmpty() }
                    ?: "0")

                val fakeLocation = FakeLocationListModel(
                    xInput.text.toString().toDouble(),
                    yInput.text.toString().toDouble(),
                    eciInput.text.toString().toInt(),
                    pciInput.text.toString().toInt(),
                    tacInput.text.toString().toInt(),
                    earfcnInput.text.toString().toInt(),
                    bandwidthInput.text.toString().toInt()
                )

                oneAdapter.add(fakeLocation)
                fakeLocationList.add(fakeLocation)
                ConfigGateway.get().writeFakeLocationList(fakeLocationList)

            }
            negativeButton(R.string.custom_location_dialog_notsave)
        }
    }

    private fun updateConfigFakeLocations(){
        oneAdapter.data
        ConfigGateway.get().writeFakeLocationList(fakeLocationList)
    }

    private fun refresh() {
        thread {
            initFakeLocationListView()
            runOnMainThread { refreshLayout.finishRefresh() }
        }
    }

    private fun initFakeLocationListView() {
        fakeLocationList =
            ConfigGateway.get().readFakeLocationList() as MutableList<FakeLocationListModel>
        runOnMainThread {
            oneAdapter.setItems(fakeLocationList)
        }
    }


    @OptIn(ExperimentalStdlibApi::class)
    inner class FakeLocationListModule(context: Context) : ItemModule<FakeLocationListModel>() {

        val ICON_MARGIN = 50

        val context: Context = context

        init {
            val fakeLocation: FakeLocation = ConfigGateway.get().readFakeLocation()

            config {
                layoutResource = R.layout.fakelocation_list_mode
            }
            onBind { model, viewBinder, metadata ->
                val latLong = viewBinder.findViewById<TextView>(R.id.lat_long_tv)
                val other = viewBinder.findViewById<TextView>(R.id.other_tv)
                val fakeImg = viewBinder.findViewById<ImageView>(R.id.fake_img)
                latLong.text = "${model.y},${model.x}"
                other.text =
                    "${model.eci},${model.pci},${model.tac},${model.earfcn},${model.pci},${model.tac},${model.earfcn},${model.bandwidth}"
                fakeImg.visibility =
                    if (isFakeLocationSame(fakeLocation, model)) View.VISIBLE else View.GONE
            }
            eventHooks += ClickEventHook<FakeLocationListModel>().apply {
                onClick { model, viewBinder, metadata ->
                    val fakeImg = viewBinder.findViewById<ImageView>(R.id.fake_img)
                    fakeImg.visibility =
                        if (isFakeLocationSame(fakeLocation, model)) View.VISIBLE else View.GONE

                    ConfigGateway.get().writeFakeLocation(
                        model.x,
                        model.y,
                        model.eci,
                        model.pci,
                        model.tac,
                        model.earfcn,
                        model.bandwidth,
                    )
                }
            }
            eventHooks += SwipeEventHook<FakeLocationListModel>().apply {
                config {
                    swipeDirection = listOf(SwipeEventHook.SwipeDirection.Start)
                }
                onSwipe { canvas, xAxisOffset, viewBinder ->
                    when {
                        xAxisOffset < 0 -> paintSwipeLeft(canvas, xAxisOffset, viewBinder.rootView)
                    }
                }
                onSwipeComplete { model, _, metadata ->
                    when (metadata.swipeDirection) {
                        SwipeEventHook.SwipeDirection.Start -> oneAdapter.remove(model)
                    }
                }
            }
            onUnbind { model, viewBinder, metadata ->

            }
        }

        private fun isFakeLocationSame(
            fakeLocation: FakeLocation,
            model: FakeLocationListModel
        ): Boolean {
            val yx = "${fakeLocation.y},${fakeLocation.x}"
            val modelyx = "${model.y},${model.x}"
            return yx === modelyx
        }

        private fun paintSwipeLeft(canvas: Canvas, xAxisOffset: Float, rootView: View) {
            val icon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white_24)
            val colorDrawable = ColorDrawable(Color.RED)

            icon?.let {
                val middle = rootView.bottom - rootView.top
                var top = rootView.top
                var bottom = rootView.bottom
                var right = rootView.right
                var left = rootView.right + xAxisOffset.toInt()
                colorDrawable.setBounds(left, top, right, bottom)
                colorDrawable.draw(canvas)

                top = rootView.top + (middle / 2) - (it.intrinsicHeight / 2)
                bottom = top + it.intrinsicHeight
                right = rootView.right - ICON_MARGIN
                left = right - it.intrinsicWidth
                it.setBounds(left, top, right, bottom)
                it.draw(canvas)
            }
        }
    }

    class EmptyListModule : EmptinessModule() {
        init {
            config {
                layoutResource = R.layout.empty_app_list
            }
        }
    }
}