package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.json.JSONArray
import org.json.JSONObject

// 1. Parking Detail (1:1 with Memory)
@Entity(
    tableName = "parking_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ParkingDetail(
    @PrimaryKey val memoryId: Int,
    val floorLevel: String,
    val slotNumber: String,
    val parkingName: String,
    val gpsLocationDescription: String,
    val vehicleName: String,
    val photoPath: String? = null
)

// 2. Money Detail (1:1 with Memory)
@Entity(
    tableName = "money_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MoneyDetail(
    @PrimaryKey val memoryId: Int,
    val personName: String,
    val amount: Double,
    val currency: String,
    val type: String, // "Lent", "Borrowed"
    val dueDate: Long? = null,
    val status: String, // "Pending", "Returned"
    val receiptPhotoPath: String? = null
)

// 3. Document Detail (1:1 with Memory)
@Entity(
    tableName = "document_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DocumentDetail(
    @PrimaryKey val memoryId: Int,
    val documentType: String, // "Passport", "Aadhaar", "PAN", "Driving License", "RC", "Insurance", "Other"
    val documentNumber: String,
    val issuedBy: String,
    val issueDate: Long? = null,
    val expiryDate: Long? = null,
    val photoPath: String? = null
)

// 4. Medicine Detail (1:1 with Memory)
@Entity(
    tableName = "medicine_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicineDetail(
    @PrimaryKey val memoryId: Int,
    val medicineName: String,
    val dosage: String,
    val morning: Boolean,
    val afternoon: Boolean,
    val night: Boolean,
    val doctorName: String,
    val prescriptionPhotoPath: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

// 5. Shopping Detail (1:1 with Memory)
@Entity(
    tableName = "shopping_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShoppingDetail(
    @PrimaryKey val memoryId: Int,
    val store: String,
    val shoppingItems: String, // Delimited by newlines instead of JSON columns
    val budget: Double? = null,
    val completed: Boolean = false,
    val purchaseDate: Long? = null
)

// 6. Place Detail (1:1 with Memory)
@Entity(
    tableName = "place_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaceDetail(
    @PrimaryKey val memoryId: Int,
    val placeName: String,
    val address: String,
    val gpsLocation: String,
    val contactPerson: String,
    val website: String,
    val photoPath: String? = null
)

// 7. Gift Detail (1:1 with Memory)
@Entity(
    tableName = "gift_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GiftDetail(
    @PrimaryKey val memoryId: Int,
    val forPerson: String,
    val occasion: String,
    val budget: Double? = null,
    val purchaseStatus: String // "Pending", "Purchased"
)

// 8. Wishlist Detail (1:1 with Memory)
@Entity(
    tableName = "wishlist_details",
    foreignKeys = [
        ForeignKey(
            entity = Memory::class,
            parentColumns = ["id"],
            childColumns = ["memoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WishlistDetail(
    @PrimaryKey val memoryId: Int,
    val productName: String,
    val expectedPrice: Double? = null,
    val websiteStore: String,
    val priority: String, // "Low", "Medium", "High"
    val photoPath: String? = null
)

// Aggregate POJO for relational Composition queries
data class MemoryWithDetails(
    @Embedded val memory: Memory,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val parkingDetail: ParkingDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val moneyDetail: MoneyDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val documentDetail: DocumentDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val medicineDetail: MedicineDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val shoppingDetail: ShoppingDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val placeDetail: PlaceDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val giftDetail: GiftDetail? = null,
    @Relation(parentColumn = "id", entityColumn = "memoryId")
    val wishlistDetail: WishlistDetail? = null
) {
    // Dynamic read-only delegates for backward compatibility with Card views and common search filters
    val person: String?
        get() = moneyDetail?.personName ?: giftDetail?.forPerson

    val amount: Double?
        get() = moneyDetail?.amount ?: giftDetail?.budget

    val isPaid: Boolean
        get() = moneyDetail?.status?.lowercase() == "returned"

    val parkingFloor: String?
        get() = parkingDetail?.floorLevel

    val parkingSlot: String?
        get() = parkingDetail?.slotNumber

    val location: String?
        get() = parkingDetail?.gpsLocationDescription ?: placeDetail?.gpsLocation

    val documentExpiry: Long?
        get() = documentDetail?.expiryDate

    val medicineDoseMorning: Boolean
        get() = medicineDetail?.morning ?: false

    val medicineDoseAfternoon: Boolean
        get() = medicineDetail?.afternoon ?: false

    val medicineDoseNight: Boolean
        get() = medicineDetail?.night ?: false

    val urlLink: String?
        get() = wishlistDetail?.websiteStore ?: placeDetail?.website

    val price: Double?
        get() = wishlistDetail?.expectedPrice

    // Dynamically map newline-separated shopping items into JSON to seamlessly satisfy existing card checklist renderers
    val checklistJson: String?
        get() = shoppingDetail?.let { shop ->
            if (shop.shoppingItems.isEmpty()) return@let null
            try {
                val array = JSONArray()
                shop.shoppingItems.split("\n").filter { it.isNotBlank() }.forEach { line ->
                    val obj = JSONObject()
                    // Format: "itemName|checkedState"
                    val parts = line.split("|")
                    val name = parts.getOrNull(0) ?: line
                    val checked = parts.getOrNull(1)?.toBoolean() ?: false
                    obj.put("text", name)
                    obj.put("checked", checked)
                    array.put(obj)
                }
                array.toString()
            } catch (e: Exception) {
                null
            }
        }
}
