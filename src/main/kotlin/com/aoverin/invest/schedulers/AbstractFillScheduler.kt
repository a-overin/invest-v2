package com.aoverin.invest.schedulers

import com.aoverin.invest.configurations.FillConfiguration
import com.aoverin.invest.exceptions.RequestApiBlockingException
import com.aoverin.invest.models.FillResult
import com.aoverin.invest.models.FillerType
import com.aoverin.invest.models.Stock
import com.aoverin.invest.services.FillSettingsService
import org.slf4j.Logger
import java.time.LocalDate

abstract class AbstractFillScheduler(
    private val fillConfiguration: FillConfiguration,
    private val settingsService: FillSettingsService,
) {

    abstract val logger: Logger

    protected fun fillInfoByDates(stock: Stock) {
        logger.info("start fill ${getFillType()} for ${stock.code}")
        settingsService.getDatesToFill(stock, getFillType(), fillConfiguration.getPeriodForFill())
            .also { logger.info("need fill ${getFillType()} for dates: $it") }
            .forEach { date -> fillOneStock(stock, date) }
        logger.info("finished fill ${getFillType()} for ${stock.code}")
    }

    private fun fillOneStock(stock: Stock, date: LocalDate) {
        logger.info("start fill ${getFillType()} for ${stock.code} and date: $date")
        runCatching {
            makeWorkForStockByDate(stock, date)
        }
            .onSuccess { settingsService.saveResultToLog(stock, getFillType(), date, FillResult.SUCCESS) }
            .onFailure {
                if (it is RequestApiBlockingException) {
                    logger.info("maximum api request error on ${stock.code} and $date")
                    throw it
                }
                logger.error("error while fill ${getFillType()} for ${stock.code} and $date with message: ${it.message}")
                settingsService.saveResultToLog(stock, getFillType(), date, FillResult.FAILED)
            }
        logger.info("finished fill ${getFillType()} for ${stock.code} and date: $date")
    }

    abstract fun makeWorkForStockByDate(stock: Stock, date: LocalDate)

    abstract fun getFillType(): FillerType
}