from pydot import Dot, Edge, Node, Subgraph

###############################################################################
# Graph
###############################################################################
interrupt = Dot(
  label="Interrupt Paths in an AIA System",
  rankdir="LR",
  splines="ortho",
)
interrupt.set_node_defaults(shape="box")
configure = Dot(
  label="Configuration Paths in an AIA System",
  rankdir="RL",
  splines="ortho",
)
configure.set_node_defaults(shape="box")

###############################################################################
# Nodes and Subgraphs
###############################################################################
msi_devices = [
  Node("msi_device_0", label="MSI Device 0"),
  Node("msi_device_1", label="MSI Device 1"),
  Node("msi_device__", label="MSI Device ..."),
]
for msi_device in msi_devices:
  interrupt.add_node(msi_device)

class APLIC(Subgraph):
  def __init__(self):
    Subgraph.__init__(self, "aplic", label="APLIC", cluster=True,
      style='filled', bgcolor="#F8CECC", pencolor="#B85450",
    )
    self.domains = [
      Node("m_domain", label="M Domain", height=1.5),
      Node("s_domain", label="S Domain", height=1.5),
    ]
    for domain in self.domains:
      self.add_node(domain)
aplic = APLIC()
interrupt.add_subgraph(aplic)
configure.add_subgraph(aplic)

wired_devices = [
  Node("wired_device_0", label="Wired Device 0"),
  Node("wired_device_1", label="Wired Device 1"),
  Node("wired_device__", label="Wired Device ..."),
]
for wired_device in wired_devices:
  interrupt.add_node(wired_device)

bus_network = Node("bus_network", label="Bus", height=7)
interrupt.add_node(bus_network)
configure.add_node(bus_network)

class IMSICHart(Subgraph):
  class IMSIC(Subgraph):
    def __init__(self, id, suffix):
      Subgraph.__init__(self, f"imsic_{suffix}", label=f"IMSIC {id}", cluster=True,
        style="filled", bgcolor="#F8CECC", pencolor="#B85450",
      )
      self.intFiles = [
        Node(f"imsic_{suffix}_mint_file", label="M IntFile"),
        Node(f"imsic_{suffix}_sint_file", label="S IntFile"),
      ]
      self.intFiles += [
        Node(f"imsic_{suffix}_vsint_file_0", label=f"VS IntFile 0"),
        Node(f"imsic_{suffix}_vsint_file__", label=f"VS IntFile ..."),
      ]
      for intFile in self.intFiles:
        self.add_node(intFile)

  def __init__(self, id, suffix):
    Subgraph.__init__(self, f"imsic_hart_{suffix}", label="", cluster=True,
      pencolor="transparent",
    )
    self.imsic = self.IMSIC(id, suffix)
    self.add_subgraph(self.imsic)
    self.hart = Node(f"hart_{suffix}", label=f"Hart {id}", height=3.2)
    self.add_node(self.hart)

imsic_harts = [IMSICHart(0, 0), IMSICHart("...", "_")]
for imsic_hart in imsic_harts:
  interrupt.add_subgraph(imsic_hart)
  configure.add_subgraph(imsic_hart)

###############################################################################
# Edges
###############################################################################
for msi_device in msi_devices:
  interrupt.add_edge(Edge(msi_device, bus_network))
for domain in aplic.domains:
  interrupt.add_edge(Edge(domain, bus_network))
  configure.add_edge(Edge(bus_network, domain))
interrupt.add_edge(Edge(aplic.domains[1], bus_network))
interrupt.add_edge(Edge(aplic.domains[0], aplic.domains[1], constraint=False))
interrupt.add_edge(Edge(aplic.domains[0], aplic.domains[1], constraint=False))
for wired_device in wired_devices:
  interrupt.add_edge(Edge(wired_device, aplic.domains[0]))
for imsic_hart in imsic_harts:
  imsic = imsic_hart.imsic
  hart = imsic_hart.hart
  for intFile in imsic.intFiles:
    interrupt.add_edge(Edge(intFile, hart))
    configure.add_edge(Edge(hart, intFile))
    interrupt.add_edge(Edge(bus_network, intFile))
    configure.add_edge(Edge(intFile, bus_network, color="transparent"))
  configure.add_edge(Edge(hart, bus_network))
###############################################################################
# Output
###############################################################################
interrupt.write(__file__.replace(".dot.py", ".py.dot"))
configure.write(__file__.replace(".dot.py", ".configure.py.dot"))
