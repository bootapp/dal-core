package com.bootapp.dal.core.utils.idgen.impl;

/*
Thread num,   nonCAS v.s CAS
1    thread: QPS: 4001563 v.s 3974383;  avgTime(ms):  0.00019288 v.s. 0.000190220
4    thread: QPS: 3972589 v.s 4032258;  avgTime(ms):  0.00089240 v.s. 0.000845000
8    thread: QPS: 3810431 v.s 4027588;  avgTime(ms):  0.00195459 v.s. 0.001655330
100  thread: QPS: 3118623 v.s 3479442;  avgTime(ms):  0.02729554 v.s. 0.009070220
1000 thread: QPS: 3249761 v.s 2363258;  avgTime(ms):  0.20897113 v.s. 0.010685175
*/

import com.bootapp.dal.core.domain.IDGen;
import com.bootapp.dal.core.repository.IDGenRepository;
import com.bootapp.dal.core.utils.idgen.IDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLongArray;

@Component
@Transactional
public class CASSnowFlakeGenerator implements IDGenerator {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final IDGenRepository genRepository;
    private static final Long START_STAMP = 1554355855000L;
    // sig[1] | timestamp[41] | data center[5] | machine [5] | sequence [12]
    private static final int MACHINE_SHL = 12;
    private static final int DATA_CENTER_SHL = 17;
    private static final int TIMESTAMP_SHL = 22;
    private static final int MAX_DATA_CENTER_NUM = 31;
    private static final int MAX_MACHINE_NUM = 31;
    private static final long MAX_SEQUENCE_NUM = 4095L;
    private static final long GEN_ID_RENEW_WINDOW = 12 * 60 * 60 * 1000L;
    private static final int SEQUENCE_RING_CAPACITY = 200;

    private Long dataCenterId = 0L;

    private AtomicLongArray sequences = new AtomicLongArray(SEQUENCE_RING_CAPACITY);

    private IDGen genInstance = null;

    public CASSnowFlakeGenerator(IDGenRepository genRepository) {
        this.genRepository = genRepository;
        genInstance = new IDGen(genRepository.count(), 0L);
        if(genInstance.getId() > MAX_MACHINE_NUM) {
            logger.error("too many machines in a single data center!");
            System.exit(-1);
        }
        renewGenId();
    }

    @Scheduled(initialDelay = GEN_ID_RENEW_WINDOW, fixedRate = GEN_ID_RENEW_WINDOW)
    private void renewGenId() {
        this.genInstance.setUpdateAt(System.currentTimeMillis());
        genRepository.save(this.genInstance);
    }

    public Long nextId() {
        do {
            Long curTimestamp = System.currentTimeMillis() - START_STAMP;
            Long index = curTimestamp % SEQUENCE_RING_CAPACITY;
            int idx = index.intValue();
            long lastId = sequences.get(idx);
            Long lastTimestamp = lastId << TIMESTAMP_SHL;
            if (lastTimestamp == 0L || lastTimestamp < curTimestamp) {
                long newId = (curTimestamp << TIMESTAMP_SHL) |
                        (dataCenterId << DATA_CENTER_SHL) |
                        (genInstance.getId() << MACHINE_SHL);
                if (sequences.compareAndSet(idx, lastId, newId)) {
                    return newId;
                }
            } else if (lastTimestamp.equals(curTimestamp)) {
                long sequence = lastId & MAX_SEQUENCE_NUM;
                if (sequence < MAX_SEQUENCE_NUM) {
                    long newId = lastId + 1L;
                    if (sequences.compareAndSet(idx, lastId, newId)) {
                        return newId;
                    }
                }
            }
        } while (true);
    }

}
