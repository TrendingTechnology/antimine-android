package dev.lucasnlm.antimine.about.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.core.viewmodel.IntentViewModel

class AboutViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
) : IntentViewModel<AboutEvent, AboutState>() {

    override fun onEvent(event: AboutEvent) {
        if (event == AboutEvent.SourceCode) {
            openSourceCode()
        }
    }

    private fun getTranslatorsList() = mapOf(
        "Arabic" to sequenceOf("Ahmad Alkurbi"),
        "Bulgarian" to sequenceOf("Georgi Eftimov"),
        "Chinese Simplified" to sequenceOf("linsui", "yilinzhao2020"),
        "Catalan" to sequenceOf("dmanye", "Archison"),
        "Czech" to sequenceOf("novas78@xda"),
        "Dutch" to sequenceOf("Max Pietersma"),
        "English" to sequenceOf("miguelsouza2212"),
        "Finnish" to sequenceOf("Topusku"),
        "French" to sequenceOf("Just Humeau"),
        "German" to sequenceOf("Oswald Boelcke", "wlls_ftn", "Trafalgar-Square"),
        "Greek" to sequenceOf("Retrial"),
        "Hungarian" to sequenceOf("Hermann Márk"),
        "Italian" to sequenceOf("Mattia - MisterWeeMan", "Nicola Lorenzetti"),
        "Japanese" to sequenceOf("Ryota Hasegawa"),
        "Norwegian" to sequenceOf("Eule Hecking"),
        "Polish" to sequenceOf("Sebastian Jasiński", "Sebastian Skibiński"),
        "Portuguese (BR)" to sequenceOf("Lucas Lima"),
        "Russian" to sequenceOf("gaich@xda", "ask45t", "Ekaterina543"),
        "Spanish" to sequenceOf("Alfredo Jara", "Aldo Rodriguez", "Inail"),
        "Turkish" to sequenceOf("Fatih Fırıncı"),
        "Ukrainian" to sequenceOf("Dmitry Shuba"),
        "Vietnamese" to sequenceOf("pnhpnh")
    ).map {
        TranslationInfo(it.key, it.value)
    }.toList()

    private fun getLicensesList() = mapOf(
        "Android SDK License" to R.raw.android_sdk,
        "Material Design Icons" to R.raw.apache2,
        "Dagger Hilt" to R.raw.apache2,
        "Moshi" to R.raw.apache2,
        "Mockito" to R.raw.mockito,
        "Sounds" to R.raw.sounds
    ).map {
        License(it.key, it.value)
    }.toList()

    private fun openSourceCode() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    override fun initialState(): AboutState =
        AboutState(
            translators = getTranslatorsList(),
            licenses = getLicensesList()
        )

    companion object {
        private const val SOURCE_CODE = "https://github.com/lucasnlm/antimine-android"
    }
}
