package serverDb.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/service")
public class ServiceController {

    @Autowired
    private ServiceApp serviceApp;

    @Autowired
    public ServiceController(ServiceApp serviceApp) {
        this.serviceApp = serviceApp;
    }


    @PostMapping(path = "/clear")
    public ResponseEntity clearDatabase() {

        return serviceApp.clearDatabase();
    }


    @GetMapping(path = "/status")
    public ResponseEntity getDatabaseInfo() {

        return serviceApp.getDatabaseInfo();
    }

}