package fuck.location.app.ui.models

import android.graphics.drawable.Drawable
import com.idanatz.oneadapter.external.interfaces.Diffable
import kotlin.random.Random

class FakeLocationListModel(
    x: Double,
    y: Double,
    eci: Int,
    pci: Int,
    tac: Int,
    earfcn: Int,
    bandwidth: Int
) : FakeLocation(x, y, eci, pci, tac, earfcn, bandwidth), Diffable {
    private val id: Long = Random.Default.nextLong()

    override fun areContentTheSame(other: Any): Boolean {
        return other is FakeLocationListModel && "${y},${x}" == "${other.y},${other.x}"
    }

    override val uniqueIdentifier: Long = id
}