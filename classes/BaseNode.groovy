import org.arl.fjage.*;
import org.arl.unet.*;
import org.arl.fjage.param.Parameter;
import org.arl.unet.DatagramReq;
import org.arl.fjage.WakerBehavior;
import org.arl.fjage.OneShotBehavior;

class BaseNode extends UnetAgent {
  def loc;
  final String title = 'Base Node';
  final String description = 'Serves as the base for a network of nodes';

  enum BaseParams implements Parameter {
    delayLength,
    neighbours
  }

  class Protocols {
    final static int INIT = 32;
    final static int ACK = 33;
    final static int BASE = 34;
    final static int RELAY = 35;
  }

  PDU init = PDU.withFormat {
    int16('address');
    uint16('delayLength');
    int16('depth');
  };

  PDU ack = PDU.withFormat {
    int16('address');
    uint32('rssi');
  };

  int delayLength = 60000;
  TreeMap neighbours;
  AgentID phy;
  AgentID node;

  @Override
  void startup() {
    subscribeForService(Services.DATAGRAM);
    phy = agentForService Services.PHYSICAL;
    node = agentForService Services.NODE_INFO;
    if (phy == null) {
      phy = agentForService Services.PHYSICAL;
    }
    subscribe topic(phy);
    add new WakerBehavior(3000, {
      neighbourBroadcast();
    });

    loc = node.location
    
    add new TickerBehavior(1000, {
      loc = [loc[0]+(Math.random()/4-0.125), loc[1]+(Math.random()/4-0.125), loc[2]+(Math.random()/4-0.125)]
    });

    locationLogger()
  }

  void locationLogger()
  {
    add new TickerBehavior(60000, {
      node.location = loc
      //log.info("UpLoc"+Arrays.toString(node.location))    commented to save sim time
    });
  }

  void neighbourBroadcast() {
    add new TickerBehavior(delayLength*10, {
      neighbours = new TreeMap<Double, Integer>(Collections.reverseOrder());
      def bytes = init.encode(address:node.address, delayLength:delayLength, depth:node.location[2]);
      add new OneShotBehavior({
        phy << new ClearReq();
        phy << new DatagramReq(
          protocol: Protocols.INIT,
          data: bytes
        );
      });

      add new WakerBehavior(delayLength, {
        if(!neighbours.isEmpty())
        {
          log.info(String.valueOf(neighbours))
          log.info(String.valueOf(neighbours.get(neighbours.firstKey())))

          add new OneShotBehavior({
            phy << new ClearReq();
            phy << new DatagramReq(
              to: neighbours.get(neighbours.firstKey()),
              protocol: Protocols.BASE,
            );
          });
        }
      });
    });
  }

  @Override
  void processMessage(Message msg) {
    if (msg instanceof DatagramNtf && msg.protocol == Protocols.ACK) {
      def bytes = ack.decode(msg.data);
      neighbours.put(bytes.rssi/10000, bytes.address);
    }
    else if (msg instanceof DatagramNtf && msg.protocol == Protocols.RELAY && msg.to == node.address) {
      log.info("Received at Base Node")
    }
  }

  List<Parameter> getParameterList() {
    allOf(BaseParams);
  }
}
