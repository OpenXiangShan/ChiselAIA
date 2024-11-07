from pydot import Dot, Edge, Node, Subgraph

graph = Dot(label="Example", splines="ortho", bgcolor="transparent")
graph.set_node_defaults(shape="box")

toAIA = Node("toAIA", label="toAIA\nTLClientNode")
graph.add_node(toAIA)
toAIA_xbar = Node("toAIA_xbar", label="toAIA_xbar\nTLXbar", width=5)
graph.add_node(toAIA_xbar)
graph.add_edge(Edge(toAIA, toAIA_xbar))

imsics_fromMem_xbar = Node("imsics_fromMem_xbar", label="imsics_fromMem_xbar\nTLXbar", width=10)
graph.add_node(imsics_fromMem_xbar)
graph.add_edge(Edge(toAIA_xbar, imsics_fromMem_xbar))

imsics = [Node(f"imsic{i}{j}", label=f"imsic{i}{j}\nTLIMSIC", style="filled", fillcolor="#F8CECC", color="#B85450") for i in range(2) for j in range(2)]
maps = [Node(f"map{i}{j}", label=f"[mAddr{i}{j},sgAddr{i}{j}]\nmap{j}{j}\nTLMap\n[0x0,0x10000]") for i in range(2) for j in range(2)]
for imsic,map in zip(imsics,maps):
  graph.add_node(imsic)
  graph.add_node(map)
  graph.add_edge(Edge(imsics_fromMem_xbar, map))
  graph.add_edge(Edge(map, imsic))

aplic = Node("aplic", label="aplic\nTLAPLIC", style="filled", fillcolor="#F8CECC", color="#B85450")
graph.add_node(aplic)
graph.add_edge(Edge(toAIA_xbar, aplic))
graph.add_edge(Edge(aplic, imsics_fromMem_xbar))

graph.write(__file__.replace("_dot.py", "_py.dot"))
