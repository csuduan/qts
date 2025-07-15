from qts.common.constant import Direction,Offset,PosDirection

def get_reverse_direction(direction:PosDirection):
    return PosDirection.SHORT if direction == PosDirection.LONG else PosDirection.LONG

    
def get_pos_direction(offset:Offset,direction:Direction):
    if offset == Offset.OPEN:
        pos_dir =  PosDirection.LONG if direction==Direction.BUY else PosDirection.SHORT
    else:
        pos_dir =  PosDirection.SHORT if direction==Direction.BUY else PosDirection.LONG
    return pos_dir