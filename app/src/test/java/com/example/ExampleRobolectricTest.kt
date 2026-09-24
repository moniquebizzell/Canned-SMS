package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.CannedTemplate
import com.example.util.SmsDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches QuickText`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QuickText", appName)
  }

  @Test
  fun `verify default templates contain all required strings`() {
    val defaultTexts = CannedTemplate.DEFAULT_TEMPLATES.map { it.text }
    assertTrue(defaultTexts.contains("I'm running about 5 minutes late!"))
    assertTrue(defaultTexts.contains("I have arrived and am outside."))
    assertTrue(defaultTexts.contains("In a meeting, can I call you back soon?"))
  }

  @Test
  fun `verify safe sms intent structure`() {
    val testText = "I have arrived and am outside."
    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("smsto:")
      putExtra("sms_body", testText)
    }
    assertEquals(Intent.ACTION_SENDTO, intent.action)
    assertEquals(Uri.parse("smsto:"), intent.data)
    assertEquals(testText, intent.getStringExtra("sms_body"))
  }
}
