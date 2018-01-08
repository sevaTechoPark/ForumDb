package serverDb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service")
public class ServiceController {

    private ServiceService serviceService;

    @Autowired
    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }


    @PostMapping(path = "/clear")
    public ResponseEntity clearDatabase() {

        return serviceService.clearDatabase();
    }


    @GetMapping(path = "/status")
    public ResponseEntity getDatabaseInfo() {

        return serviceService.getDatabaseInfo();
    }

}