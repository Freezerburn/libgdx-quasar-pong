package com.unlockeddoors.pong;

import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.DelayedVal;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 2/23/14
 * Time: 1:27 PM
 */
public class Wall extends BasicActor<Event, Void> {
    private static int ID = 0;

    boolean going = true;
    Rectangle rect;
    Pong.ShapeRunnableRemover remover;
    Val<Rectangle> val = new Val<>();

    public Wall(Rectangle rect) {
        super("Wall" + ID++, Pong.MAILBOX_CONFIG);

        this.rect = rect;
        val.set(rect);
        remover = Pong.postShapeRunnable(
                () -> Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height),
                ShapeRenderer.ShapeType.Filled
        );
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        while(going) {
            final Event e = receive();
            switch (e.type) {
                case REQUEST_NAME:
                    RequestReplyHelper.reply(e, "Wall");
                    break;
                case REQUEST_RECT:
                    RequestReplyHelper.reply(e, val);
                    break;
                case KILL:
                    going = false;
                    break;
            }
        }
        remover.remove();
        return null;
    }
}
