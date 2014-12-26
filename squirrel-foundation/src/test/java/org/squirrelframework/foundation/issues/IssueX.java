package org.squirrelframework.foundation.issues;

import org.junit.Test;
import org.squirrelframework.foundation.fsm.AnonymousAction;
import org.squirrelframework.foundation.fsm.HistoryType;
import org.squirrelframework.foundation.fsm.StateMachineBuilder;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.annotation.ContextInsensitive;
import org.squirrelframework.foundation.fsm.impl.AbstractStateMachine;

import static org.junit.Assert.assertEquals;

public class IssueX {
    @Test
    public void performsTransactionOnEventFromAction() {
        FSM fsm = createFSM();
        fsm.fire(Event.Start);
        fsm.fire(Event.GoWild);
        assertEquals(State.GoneWild, fsm.getCurrentState());
        fsm.fire(Event.CalmDown);
        assertEquals(State.Calm, fsm.getCurrentState());
        fsm.fire(Event.Stop);
        assertEquals(State.Idle, fsm.getCurrentState());
    }

    private static FSM createFSM() {
        StateMachineBuilder<FSM, State, Event, Void> builder =
                StateMachineBuilderFactory.create(FSM.class, State.class, Event.class, Void.class);

        builder.externalTransition().from(State.Idle).to(State.Working).on(Event.Start);
        builder.externalTransition().from(State.Working).to(State.Idle).on(Event.Stop);

        builder.defineSequentialStatesOn(State.Working, HistoryType.NONE,
                State.Calm, State.GoingWild, State.GoneWild
        );
        builder.externalTransition().from(State.Calm).to(State.GoingWild).on(Event.GoWild);
        builder.externalTransition().from(State.GoingWild).to(State.GoneWild).on(Event.GoneWild);
        builder.externalTransition().from(State.GoneWild).to(State.Calm).on(Event.CalmDown);


        builder.onEntry(State.Idle).perform(new LoggerAction("Idle entry"));
        builder.onExit(State.Idle).perform(new LoggerAction("Idle exit"));
        builder.onEntry(State.Working).perform(new LoggerAction("Working entry"));
        builder.onExit(State.Working).perform(new LoggerAction("Working exit"));

        builder.onEntry(State.Calm).perform(new LoggerAction("Calm entry"));
        builder.onExit(State.Calm).perform(new LoggerAction("Calm exit"));
        builder.onEntry(State.GoingWild).perform(new LoggerAction("GoingWild entry") {
            @Override
            public void execute(State from, State to, Event event, Void context, FSM stateMachine) {
                super.execute(from, to, event, context, stateMachine);
                // let's assume we really should stop now instead of going wild
                stateMachine.fire(Event.GoneWild);
            }
        });
        builder.onExit(State.GoingWild).perform(new LoggerAction("GoingWild exit"));
        builder.onEntry(State.GoneWild).perform(new LoggerAction("GoneWild entry"));
        builder.onExit(State.GoneWild).perform(new LoggerAction("GoneWild exit"));
        return builder.newStateMachine(State.Idle);
    }

    private static class LoggerAction extends AnonymousAction<FSM, State, Event, Void> {
        private final String str;

        public LoggerAction(String str) {
            this.str = str;
        }

        @Override
        public void execute(State from, State to, Event event, Void context, FSM stateMachine) {
            System.err.println(str);
        }
    }

    @ContextInsensitive
    private static class FSM extends AbstractStateMachine<FSM, State, Event, Void> {
    }

    private static enum State {
        Idle,
        Working,
            GoneWild,
            GoingWild,
            Calm
    }

    private static enum Event {
        Start,
        GoneWild,
        GoWild,
        CalmDown,
        Stop
    }
}

