package zs.live

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@Service
@Order(0)
class APP {
    @Autowired
    ApplicationContext context;

    static APP instance

    @PostConstruct
    @Order(0)
    void init() { instance = this; }
}
