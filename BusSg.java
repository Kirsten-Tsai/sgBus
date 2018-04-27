import java.util.Optional;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;

/**
 * A BusSg class encapsulate the data related to the bus services and
 * bus stops in Singapore, and supports queries to the data.
 */
class BusSg {

  /**
   * Given a bus stop and a name, find the bus services that serve between
   * the given stop and any bus stop with matching mame.
   * @param  stop The bus stop
   * @param  name The (partial) name of other bus stops.
   * @return The (optional) bus routes between the stops.
   */
  public static Optional<BusRoutes> findBusServicesBetween(BusStop stop, String name) {
    if (stop == null || name == null) {
      return Optional.empty();
    }

    return CompletableFuture.supplyAsync(() -> stop.getBusServices())
        .thenApply(allServices -> {
          Map<BusService,Set<BusStop>> validServices = new HashMap<>();
          Map<BusService, CompletableFuture<Set<BusStop>>> cfMap = new HashMap<>();
          for (BusService service : allServices) { 
            cfMap.put(service, CompletableFuture.supplyAsync(() -> service.findStopsWith(name)));
          }
          for (Map.Entry<BusService, CompletableFuture<Set<BusStop>>> entry : cfMap.entrySet()) {
            Set<BusStop> stops = entry.getValue().join();
            if (!stops.isEmpty()) {
              validServices.put(entry.getKey(), stops);
            }
          }
          return validServices;
        })
        .handle((validServices, exception) -> {
          if (exception != null) {
            System.err.println("Unable to complete query: " + exception.getMessage());
            validServices = new HashMap<>();              
          }
          return validServices;
        })
        .thenApply(validServices -> {
          return Optional.of(new BusRoutes(stop, name, validServices));
        }).join();
  }
}
