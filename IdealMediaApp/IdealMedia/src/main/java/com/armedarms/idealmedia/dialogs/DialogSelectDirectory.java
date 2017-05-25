package com.armedarms.idealmedia.dialogs;

import android.app.Dialog;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.armedarms.idealmedia.R;
import com.armedarms.idealmedia.utils.ResUtils;
import com.r0adkll.postoffice.PostOffice;
import com.r0adkll.postoffice.model.Delivery;
import com.r0adkll.postoffice.model.Design;
import com.r0adkll.postoffice.ui.SupportMail;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.r0adkll.postoffice.styles.ListStyle.Builder;

public class DialogSelectDirectory
{

    public interface Result {
        void onChooseDirectory(String dir);
        void onCancelChooseDirectory();
    }

    private final Delivery mDelivery;
    private TextView mMessage;

    List<File> m_entries = new ArrayList< File >();
    File m_currentDir;
    Context m_context;
    Result m_result = null;

    public class DirectoryAdapter extends ArrayAdapter<File> {
        public DirectoryAdapter(int resid) {
            super( m_context, resid, m_entries );
        }

        // This function is called to show each view item
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView textview = (TextView) super.getView( position, convertView, parent );

            if ( m_entries.get(position) == null )
            {
                textview.setText( ".." );
                textview.setCompoundDrawablesWithIntrinsicBounds( m_context.getResources().getDrawable( R.drawable.ic_notification ), null, null, null );
            }
            else
            {
                textview.setText( m_entries.get(position).getName() );
                textview.setCompoundDrawablesWithIntrinsicBounds( null, null, null, null );
            }

            return textview;
        }
    }

    private void enlistDirectories()
    {
        m_entries.clear();

        // Get files
        File[] files = m_currentDir.listFiles();

        // Add the ".." entry
        if ( m_currentDir.getParent() != null )
            m_entries.add( new File("..") );

        if ( files != null )
        {
            for ( File file : files )
            {
                if ( !file.isDirectory() || !file.canRead())
                    continue;

                m_entries.add( file );
            }
        }

        Collections.sort(m_entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });
    }

    public DialogSelectDirectory(final Context context, FragmentManager fragmentManager, Result result, String startDir)
    {
        m_context = context;
        m_result = result;

        if ( startDir != null )
            m_currentDir = new File( startDir );
        else
            m_currentDir = Environment.getExternalStorageDirectory();

        enlistDirectories();

        final DirectoryAdapter adapter = new DirectoryAdapter(android.R.layout.simple_list_item_1);

        mDelivery = PostOffice.newMail(context)
                .setTitle(context.getString(R.string.dlg_choosedir_title))
                .setMessage(m_currentDir.toString())
                .setDesign(Design.MATERIAL_LIGHT)
                .setCancelable(true)
                .setButtonTextColor(Dialog.BUTTON_POSITIVE, ResUtils.resolve(context, R.attr.colorPositive))
                .setButton(Dialog.BUTTON_POSITIVE, android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (m_result != null)
                            m_result.onChooseDirectory(m_currentDir.getAbsolutePath());
                        dialogInterface.dismiss();
                    }
                })
                .setButton(Dialog.BUTTON_NEGATIVE, android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (m_result != null)
                            m_result.onCancelChooseDirectory();
                        dialogInterface.dismiss();
                    }
                })
                .setStyle(new Builder(context)
                                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                        if (position < 0 || position >= m_entries.size())
                                            return;

                                        if (m_entries.get(position).getName().equals(".."))
                                            m_currentDir = m_currentDir.getParentFile();
                                        else
                                            m_currentDir = m_entries.get(position);

                                        setMessage(m_currentDir.toString());

                                        enlistDirectories();

                                        adapter.notifyDataSetChanged();
                                    }
                                })
                                .build(adapter)
                )
                .build();
        mDelivery.show(fragmentManager);

        /*
        final Dialog dialog = new Dialog(context, R.style.FullHeightDialog);

        View view = ((Activity)context).getLayoutInflater().inflate(R.layout.dialog_dirlist, null);
        TextView title = (TextView) view.findViewById(R.id.dlgtitle);
        Button btnOk = (Button) view.findViewById(R.id.buttonOk);
        editText = (EditText) view.findViewById(R.id.editText);
        editText.setText(m_currentDir.toString());
        title.setText(context.getString(R.string.dlg_choosedir_title));
        m_list = (ListView) view.findViewById(R.id.listView);

        DirectoryAdapter adapter = new DirectoryAdapter( android.R.layout.simple_list_item_1 );

        m_list.setAdapter(adapter);

        m_list.setOnItemClickListener(this);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( m_result != null )
                    m_result.onChooseDirectory( editText.getText().toString());
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);

        dialog.show();
        */
    }

    private void setMessage(final String message) {
        if (mMessage == null) {
            try {
                // hack to set dialog message after dialog was shown
                Field field = mDelivery.getClass().getDeclaredField("mActiveSupportMail");
                field.setAccessible(true);
                SupportMail mail = (SupportMail)field.get(mDelivery);

 /*
                field = mail.getClass().getDeclaredField("mMailbox");
                field.setAccessible(true);
                Mailbox mailbox = (Mailbox)field.get(mail);
*/

                field = mail.getClass().getDeclaredField("mMessage");
                field.setAccessible(true);
                mMessage = (TextView)field.get(mail);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (mMessage != null)
            mMessage.post(new Runnable() {
                              @Override
                              public void run() {
                                  mMessage.setText(message);
                              }
                          }
            );
    }
}