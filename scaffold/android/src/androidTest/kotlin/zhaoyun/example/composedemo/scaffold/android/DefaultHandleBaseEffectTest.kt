package zhaoyun.example.composedemo.scaffold.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect

@RunWith(AndroidJUnit4::class)
class DefaultHandleBaseEffectTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun defaultHandleBaseEffect_handles_toast_effects_without_crashing() {
        composeRule.runOnUiThread {
            runBlocking {
                defaultHandleBaseEffect(
                    context = composeRule.activity,
                    effect = BaseEffect.ShowToast("toast"),
                )
            }
        }
    }

    @Test
    fun defaultHandleBaseEffect_ignores_unsupported_effects_without_crashing() {
        composeRule.runOnUiThread {
            runBlocking {
                defaultHandleBaseEffect(
                    context = composeRule.activity,
                    effect = BaseEffect.NavigateBack,
                )
            }
        }
    }
}
