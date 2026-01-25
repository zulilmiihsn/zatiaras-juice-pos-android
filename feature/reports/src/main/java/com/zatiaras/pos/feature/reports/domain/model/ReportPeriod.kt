package com.zatiaras.pos.feature.reports.domain.model

import com.zatiaras.pos.core.domain.model.DatePeriod

/**
 * Type alias for DatePeriod to maintain backward compatibility.
 * 
 * This approach follows DRY principle - the actual enum is defined
 * in core:domain and reused across modules.
 */
typealias ReportPeriod = DatePeriod
