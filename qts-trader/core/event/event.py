from enum import Enum
from typing import Any, Callable, List, Dict
from collections import defaultdict
from queue import Empty, Queue
from threading import Thread
from time import sleep

import asyncio

from qts.model.message import MsgType
class Event:
    """
    Event object consists of a type string which is used
    by event engine for distributing event, and a data
    object which contains the real data.
    """

    def __init__(self, type: MsgType, data: Any = None) -> None:
        """"""
        self.type: MsgType = type
        self.data: Any = data


HandlerType = Callable[[Event], None]

class EventEngine:
    """
    Event engine distributes event object based on its type
    to those handlers registered.

    It also generates timer event by every interval seconds,
    which can be used for timing purpose.
    """

    def __init__(self) -> None:
        self._queue: Queue = Queue()
        self._active: bool = False
        self._thread: Thread = Thread(target=self._run)
        self._handlers: defaultdict = defaultdict(list)
        self._general_handlers: List = []

    def _run(self) -> None:
        """
        Get event from queue and then process it.
        """
        while self._active:
            try:
                event: Event = self._queue.get(block=True, timeout=1)
                self._process(event)
            except Empty:
                pass

    def _process(self, event: Event) -> None:
        """
        First distribute event to those handlers registered listening
        to this type.

        Then distribute event to those general handlers which listens
        to all types.
        """
        if event.type in self._handlers:
            [handler(event) for handler in self._handlers[event.type]]

        if self._general_handlers:
            [handler(event) for handler in self._general_handlers]

    def start(self) -> None:
        """
        Start event engine to process events and generate timer events.
        """
        self._active = True
        self._thread.start()

    def stop(self) -> None:
        """
        Stop event engine.
        """
        self._active = False
        self._timer.join()
        self._thread.join()

    def put(self, event: Event) -> None:
        """
        Put an event object into event queue.
        """
        self._queue.put(event)

    def register(self, type: MsgType, handler: HandlerType ) -> None:
        """
        Register a new handler function for a specific event type. Every
        function can only be registered once for each event type.
        """
        handler_list: list = self._handlers[type]
        if handler not in handler_list:
            handler_list.append(handler)

    def unregister(self, type: str, handler: HandlerType ) -> None:
        """
        Unregister an existing handler function from event engine.
        """
        handler_list: list = self._handlers[type]

        if handler in handler_list:
            handler_list.remove(handler)

        if not handler_list:
            self._handlers.pop(type)

    def register_general(self, handler: HandlerType) -> None:
        """
        Register a new handler function for all event types. Every
        function can only be registered once for each event type.
        """
        if handler not in self._general_handlers:
            self._general_handlers.append(handler)

    def unregister_general(self, handler: HandlerType) -> None:
        """
        Unregister an existing general handler function.
        """
        if handler in self._general_handlers:
            self._general_handlers.remove(handler)
