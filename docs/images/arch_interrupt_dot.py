from arch_common import *

###############################################################################
# Graph
###############################################################################
interrupt = Dot(
  label="Interrupt Paths in an AIA System",
  rankdir="LR",
  splines="ortho",
  bgcolor="transparent",
)
interrupt.set_node_defaults(shape="box")

###############################################################################
# Nodes and Subgraphs
###############################################################################
for msi_device in msi_devices:
  interrupt.add_node(msi_device)
interrupt.add_subgraph(aplic)
for wired_device in wired_devices:
  interrupt.add_node(wired_device)
interrupt.add_node(bus_network)
for imsic_hart in imsic_harts:
  interrupt.add_subgraph(imsic_hart)

###############################################################################
# Edges
###############################################################################
for msi_device in msi_devices:
  interrupt.add_edge(Edge(msi_device, bus_network))
for domain in aplic.domains:
  interrupt.add_edge(Edge(domain, bus_network))
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
    interrupt.add_edge(Edge(bus_network, intFile))

###############################################################################
# Output
###############################################################################
interrupt.write(__file__.replace("_dot.py", "_py.dot"))
