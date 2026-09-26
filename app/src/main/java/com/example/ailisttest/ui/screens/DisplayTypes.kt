package com.example.ailisttest.ui.screens

import com.example.ailisttest.data.local.ListScreenItems
import com.example.ailisttest.data.local.TagEntity
import com.example.ailisttest.ui.MainViewModel

data class LiveDataFieldContext(
    val item: ListScreenItems,
    val tag: TagEntity?,
    val isBitTagItem: Boolean,
    val bitIndex: Int?,
    val isBitSet: Boolean,
    val isReadOnly: Boolean,
    val isTwoTouch: Boolean,
    val viewModel: MainViewModel,
    val onShowPopup: () -> Unit = {},
    val onShowSnackbar: (String) -> Unit = {}
)

abstract class LiveDataDisplayType {
    abstract val name: String
    abstract val tagDataType: String

    open fun onItemClicked(context: LiveDataFieldContext) {
        if (context.isReadOnly) {
            context.onShowSnackbar("This field is read only.")
        } else {
            context.onShowPopup()
        }
    }

    open fun onAccept(newValue: String, context: LiveDataFieldContext) {
        if (context.isReadOnly) return

        if (context.isBitTagItem) {
            val setOn = when (newValue.trim().uppercase()) {
                "1", "ON", "TRUE", "ENABLE", "ENABLED" -> true
                "0", "OFF", "FALSE", "DISABLE", "DISABLED" -> false
                else -> (newValue.toLongOrNull() ?: 0L) != 0L
            }
            context.viewModel.updateBitValueAndWrite(
                item = context.item,
                targetState = setOn,
                onShowSnackbar = context.onShowSnackbar
            )
        } else {
            val tag = context.tag ?: return
            context.viewModel.updateTagValueAndWrite(
                tag = tag,
                newValue = newValue,
                onShowSnackbar = context.onShowSnackbar
            )
        }
    }
}

// Bit ("B") Display Types
class B_On_Button : LiveDataDisplayType() {
    override val name = "On_Button"
    override val tagDataType = "B"
}

class B_Off_Button : LiveDataDisplayType() {
    override val name = "Off_Button"
    override val tagDataType = "B"
}

class B_Toggle_Button : LiveDataDisplayType() {
    override val name = "Toggle_Button"
    override val tagDataType = "B"
}

class B_Switch : LiveDataDisplayType() {
    override val name = "Switch"
    override val tagDataType = "B"
}

class B_CheckBox : LiveDataDisplayType() {
    override val name = "CheckBox"
    override val tagDataType = "B"
}

class B_Radio_Button : LiveDataDisplayType() {
    override val name = "Radio_Button"
    override val tagDataType = "B"
}

// Double Word Integer ("DD") Display Types
class DD_Default : LiveDataDisplayType() {
    override val name = "Default (Numeric entry)"
    override val tagDataType = "DD"
}

class DD_List : LiveDataDisplayType() {
    override val name = "List (Text based upon value)"
    override val tagDataType = "DD"
}

// Single Word Integer ("DS") Display Types
class DS_Default : LiveDataDisplayType() {
    override val name = "Default (Numeric entry)"
    override val tagDataType = "DS"
}

class DS_List : LiveDataDisplayType() {
    override val name = "List (Text based upon value)"
    override val tagDataType = "DS"
}

// Hexadecimal ("DH") Display Types
class DH_Default : LiveDataDisplayType() {
    override val name = "Default (Numeric entry)"
    override val tagDataType = "DH"
}

class DH_List : LiveDataDisplayType() {
    override val name = "List (Text based upon value)"
    override val tagDataType = "DH"
}

// Floating Point ("DF") Display Types
class DF_Default : LiveDataDisplayType() {
    override val name = "Default (Numeric entry)"
    override val tagDataType = "DF"
}

object DisplayTypes {
    val allHandlers: List<LiveDataDisplayType> = listOf(
        B_On_Button(),
        B_Off_Button(),
        B_Toggle_Button(),
        B_Switch(),
        B_CheckBox(),
        B_Radio_Button(),
        DD_Default(),
        DD_List(),
        DS_Default(),
        DS_List(),
        DH_Default(),
        DH_List(),
        DF_Default()
    )

    private val optionsByDataType: Map<String, List<String>> = mapOf(
        "B" to listOf("On_Button", "Off_Button", "Toggle_Button", "Switch", "CheckBox", "Radio_Button"),
        "DD" to listOf("Default (Numeric entry)", "List (Text based upon value)"),
        "DS" to listOf("Default (Numeric entry)", "List (Text based upon value)"),
        "DH" to listOf("Default (Numeric entry)", "List (Text based upon value)"),
        "DF" to listOf("Default (Numeric entry)")
    )

    fun getOptionsForDataType(shortName: String): List<String> {
        return optionsByDataType[shortName.uppercase().trim()] ?: listOf("Default (Numeric entry)", "List (Text based upon value)")
    }

    fun getHandler(tagDataType: String, displayTypeName: String): LiveDataDisplayType {
        val cleanDataType = tagDataType.uppercase().trim()
        val defaultName = if (cleanDataType == "B") "On_Button" else "Default (Numeric entry)"
        val rawName = displayTypeName.trim().ifEmpty { defaultName }

        val cleanName = when (rawName.lowercase()) {
            "default" -> "Default (Numeric entry)"
            "list" -> "List (Text based upon value)"
            "slider" -> "Switch"
            else -> rawName
        }

        return allHandlers.find {
            it.tagDataType.equals(cleanDataType, ignoreCase = true) &&
                    (it.name.equals(cleanName, ignoreCase = true) ||
                     (cleanName.startsWith("default", ignoreCase = true) && it.name.startsWith("Default", ignoreCase = true)) ||
                     (cleanName.startsWith("list", ignoreCase = true) && it.name.startsWith("List", ignoreCase = true)))
        } ?: when (cleanDataType) {
            "B" -> B_On_Button()
            "DD" -> DD_Default()
            "DS" -> DS_Default()
            "DH" -> DH_Default()
            "DF" -> DF_Default()
            else -> DS_Default()
        }
    }
}
