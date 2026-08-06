package com.vythera.omniapps.api

import android.content.Context

object MetadataManager {

    private var client: MetadataClient? = null

    fun init(context: Context): MetadataClient {
        if (client == null) {
            client = MetadataClient(
                context = context.applicationContext,
                cdnBase = "https://smartworldarafath.github.io/appstore-metadata"
            )
        }
        return client!!
    }

    fun get(): MetadataClient = client
        ?: throw IllegalStateException("MetadataManager.init() must be called before get()")
}
