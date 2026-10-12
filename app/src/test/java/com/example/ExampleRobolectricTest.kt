package com.example

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.example.util.FormatAction
import com.example.util.RichTextParser
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AMOLED Notes", appName)
  }

  @Test
  fun `test rich text bold format formatting`() {
    val initial = TextFieldValue("Hello World", TextRange(0, 5))
    val boldResult = RichTextParser.applyFormat(initial, FormatAction.BOLD)
    assertEquals("**Hello** World", boldResult.text)
  }

  @Test
  fun `test checkbox toggle`() {
    val initialText = "[ ] Buy groceries\n[x] Workout"
    val toggled = RichTextParser.toggleCheckboxAtLine(initialText, 0)
    assertTrue(toggled.startsWith("[x] Buy groceries"))
  }
}
