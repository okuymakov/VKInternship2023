package com.example.vkinternshipapp.core

import android.content.Context
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.vkinternshipapp.R
import com.example.vkinternshipapp.domain.filemanager.SortType
import com.example.vkinternshipapp.domain.models.FileModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
fun Date.format(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}

fun Long.formatFileSize(context: Context): String {
    var size = this.toDouble()
    return if (size < 1024) {
        context.resources.getString(R.string.file_size_b, size)
    } else {
        size /= 1024
        if (size < 1024) {
            context.resources.getString(R.string.file_size_kb, size)
        } else {
            size /= 1024
            if (size < 1024) {
                context.resources.getString(R.string.file_size_mb, size)
            } else {
                context.resources.getString(R.string.file_size_gb, size / 1024)
            }
        }
    }
}

fun AppCompatActivity.launchOnLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state) {
            block()
        }
    }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.showPopup(@MenuRes menuRes: Int, onClick: ((MenuItem) -> Boolean)? = null): PopupMenu {
    return PopupMenu(context, this).apply {
        setOnMenuItemClickListener(onClick)
        menuInflater.inflate(menuRes, menu)
        show()
    }
}

fun List<FileModel>.sort(
    sortType: SortType = SortType.BY_SIZE,
    isDescending: Boolean = false
): List<FileModel> {
    val comparable: (FileModel) -> Comparable<*> = {
        when (sortType) {
            SortType.BY_NAME -> it.name
            SortType.BY_SIZE -> it.size
            SortType.BY_DATE -> it.createdAt
            SortType.BY_TYPE -> it.type
        }
    }
    return sortedWith(compareBy<FileModel> { !it.isDirectory }.run {
        if (isDescending) thenByDescending(comparable) else thenBy(comparable)
    })
}

fun String.directoryName(context: Context): CharSequence {
    return if (this == Constants.ROOT_PATH) context.getString(R.string.root_dir_name)
    else substringAfterLast(File.separator)
}

suspend fun File.getHash(): String = withContext(Dispatchers.IO) {
    val messageDigest = MessageDigest.getInstance("MD5")
    val buffer = ByteArray(2048)
    inputStream().buffered().use {
        while (true) {
            val sz = it.read(buffer)
            messageDigest.update(buffer)
            if (sz <= 0) break
        }
    }

    var hash = BigInteger(1, messageDigest.digest()).toString(16)
    hash = String.format("%32s", hash).replace(' ', '0')
    Log.d("Saving hashes", "$name   hash: $hash")
    hash
}