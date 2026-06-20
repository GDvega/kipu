package pe.kipu.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pe.kipu.core.data.ocr.AndroidReceiptImageLoader
import pe.kipu.core.data.ocr.MlKitReceiptOcrEngine
import pe.kipu.core.domain.ocr.ReceiptImageLoader
import pe.kipu.core.domain.ocr.ReceiptOcrEngine

@Module
@InstallIn(SingletonComponent::class)
abstract class OcrModule {

    @Binds
    @Singleton
    abstract fun bindReceiptOcrEngine(impl: MlKitReceiptOcrEngine): ReceiptOcrEngine

    @Binds
    @Singleton
    abstract fun bindReceiptImageLoader(impl: AndroidReceiptImageLoader): ReceiptImageLoader
}
