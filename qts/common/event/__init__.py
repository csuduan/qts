from .event import EventEngine,Event
event_engine = EventEngine()
event_engine.start()

__all__ = [
    "Event",
    "event_engine",
]