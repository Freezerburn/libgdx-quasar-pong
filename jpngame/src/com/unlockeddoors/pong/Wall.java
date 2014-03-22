package com.unlockeddoors.pong;

import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.RequestReplyHelper;
import co.paralleluniverse.fibers.SuspendExecution;
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

    public Wall(Rectangle rect) {
        super("Wall" + ID++, Pong.MAILBOX_CONFIG);

        this.rect = rect;
        remover = Pong.postShapeRunnable(
                () -> Pong.shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height),
                ShapeRenderer.ShapeType.Filled
        );
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while(going) {
                final Event e = receive();
                switch (e.type) {
                    case REQUEST_RECT:
                        System.out.println(ref() + " got REQUEST_RECT event. Giving back rect: " + rect);
                        RequestReplyHelper.reply(e, rect);
                        break;
                    case COLLISIONS:
                        System.out.println("Wall arriving and deregistring render.");
                        Pong.renderSynchPoint.arriveAndDeregister();
                        break;
                    case KILL:
                        going = false;
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        remover.remove();
        return null;
    }
}
