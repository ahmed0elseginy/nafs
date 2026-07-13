package com.nafs.app.util

import android.content.Context
import android.content.res.Configuration
import com.nafs.app.domain.model.AppLanguage
import java.util.Locale

/** Applies an [AppLanguage] to a [Context] by overriding its configuration locale. */
object LocaleManager {

    fun wrap(context: Context, language: AppLanguage): Context {
        val tag = language.tag ?: return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
