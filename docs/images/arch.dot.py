from pydot import Dot, Edge, Node, Subgraph

###############################################################################
# Graph
###############################################################################
graph = Dot(
  label="Interrupt delivery by MSIs when harts have IMSICs for receiving them.",
  rankdir="LR",
  splines="ortho",
)
graph.set_node_defaults(shape="box")

###############################################################################
# Nodes and Subgraphs
###############################################################################
msi_devices = [
  Node("msi_device_0", label="MSI Device 0"),
  Node("msi_device_1", label="MSI Device 1"),
  Node("msi_device__", label="MSI Device ..."),
]
for msi_device in msi_devices:
  graph.add_node(msi_device)

aplic = Node("aplic", label="APLIC",
  height=3, style="filled, dotted", fillcolor="#F8CECC", color="#B85450",
)
graph.add_node(aplic)

wired_devices = [
  Node("wired_device_0", label="Wired Device 0"),
  Node("wired_device_1", label="Wired Device 1"),
  Node("wired_device__", label="Wired Device ..."),
]
for wired_device in wired_devices:
  graph.add_node(wired_device)

bus_network = Node("bus_network", label="Bus Network\n(Messages)", height=15)

class IMSICHart(Subgraph):
  class IMSIC(Subgraph):
    def __init__(self, id):
      Subgraph.__init__(self, f"imsic_{id}", label=f"IMSIC {id}", cluster=True,
        style="filled", bgcolor="#F8CECC", pencolor="#B85450",
      )
      self.intFiles = [
        Node(f"imsic_{id}_mint_file", label="M IntFile"),
        Node(f"imsic_{id}_sint_file", label="S IntFile"),
      ]
      self.intFiles += [
        Node(f"imsic_{id}_vsint_file_0", label=f"VS IntFile 0"),
        Node(f"imsic_{id}_vsint_file__", label=f"VS IntFile ..."),
      ]
      for intFile in self.intFiles:
        self.add_node(intFile)

  def __init__(self, id):
    Subgraph.__init__(self, f"imsic_hart_{id}", label="", cluster=True,
      pencolor="transparent",
    )
    self.imsic = self.IMSIC(id)
    self.add_subgraph(self.imsic)
    self.hart = Node(f"hart_{id}", label=f"Hart {id}", height=3.2)
    self.add_node(self.hart)

imsic_harts = [IMSICHart(i) for i in range(4)]
for imsic_hart in imsic_harts:
  graph.add_subgraph(imsic_hart)

###############################################################################
# Edges
###############################################################################
graph.add_node(bus_network)
for msi_device in msi_devices:
  graph.add_edge(Edge(msi_device, bus_network))
graph.add_edge(Edge(aplic, bus_network, style="dotted"))
for wired_device in wired_devices:
  graph.add_edge(Edge(wired_device, aplic, style="dotted"))
for imsic_hart in imsic_harts:
  imsic = imsic_hart.imsic
  hart = imsic_hart.hart
  for intFile in imsic.intFiles:
    graph.add_edge(Edge(intFile, hart))
    graph.add_edge(Edge(bus_network, intFile))

###############################################################################
# Output
###############################################################################
graph.write(__file__.replace(".dot.py", ".py.dot"))
