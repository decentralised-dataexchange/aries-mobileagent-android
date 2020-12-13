package io.igrant.mobileagent.dailogFragments

import android.R
import android.app.Dialog
import android.app.ProgressDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment

open class BaseDialogFragment : DialogFragment() {
    var progressDialog: ProgressDialog? = null
    fun showProgress() {
        progressDialog = ProgressDialog(context)
        progressDialog!!.setMessage("Please wait...")
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
    }

    fun dismissProgress() {
        progressDialog!!.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val style = STYLE_NORMAL
        val theme = R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth
        setStyle(style, theme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // request a window without the title
        dialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return dialog
    }
}
